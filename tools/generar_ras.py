#!/usr/bin/env python3
"""
Regenera src/main/resources/ras.txt y src/main/resources/criterios.txt con los
resultados de aprendizaje (RA) y sus criterios de evaluación oficiales de los
módulos profesionales, extrayéndolos del Boletín Oficial del Estado. Los dos
ficheros los leen los sembradores RasIniciales y CriteriosIniciales al arrancar
la aplicación.

Uso:
    python tools/generar_ras.py

Fuentes (textos consolidados del BOE):
    DAM   Real Decreto 450/2010 (BOE-A-2010-8067)
    DAW   Real Decreto 686/2010 (BOE-A-2010-9269)
    ASIR  Real Decreto 1629/2009
    SMR   Real Decreto 1691/2007
    TRANS Real Decreto 659/2023 (módulos transversales: 1709, 1710, 1708,
          1665, 0179, 1664 y 0156)
    PROY  Real Decreto 499/2024 (1713 Proyecto intermodular de grado medio)

El mismo código de módulo puede tener redacciones ligeramente distintas según
el Real Decreto del título (por ejemplo el 0373), así que cada ciclo se toma de
la fuente de su propio título y los transversales se comparten.

Cada línea de ras.txt es:        GRUPO|CODIGO_MODULO|ORDEN|TEXTO
Cada línea de criterios.txt es:  GRUPO|CODIGO_MODULO|ORDEN_RA|ORDEN|TEXTO
El grupo indica a qué siglas de ciclo se aplica la línea (ver RasIniciales).

Los textos se guardan tal cual (sin cortar): el criterio más largo ronda los
6000 caracteres, así que las columnas de la base de datos admiten 8000.
"""

import html
import re
import sys
import urllib.request
from pathlib import Path

CARPETA = Path(__file__).resolve().parents[1] / "src" / "main" / "resources"
SALIDA_RA = CARPETA / "ras.txt"
SALIDA_CRITERIOS = CARPETA / "criterios.txt"

FUENTES = {
    "DAM": "https://www.boe.es/buscar/act.php?id=BOE-A-2010-8067",
    "DAW": "https://www.boe.es/buscar/act.php?id=BOE-A-2010-9269",
    "ASIR": "https://www.boe.es/eli/es/rd/2009/10/30/1629",
    "SMR": "https://www.boe.es/eli/es/rd/2007/12/14/1691",
    "TRANS": "https://www.boe.es/eli/es/rd/2023/07/18/659",
    "PROY": "https://www.boe.es/diario_boe/txt.php?id=BOE-A-2024-10684",
}

# (grupo de salida, fuente BOE, códigos de módulo que se toman de esa fuente)
COBERTURA = [
    ("DAM", "DAM", ["0373", "0483", "0484", "0485", "0486", "0487", "0488",
                    "0489", "0490", "0491", "0492"]),
    ("DAW", "DAW", ["0373", "0483", "0484", "0485", "0487", "0612", "0613",
                    "0614", "0615", "0616"]),
    ("ASIR", "ASIR", ["0369", "0370", "0371", "0372", "0373", "0374", "0375",
                      "0376", "0377", "0378", "0379"]),
    ("SMR", "SMR", ["0221", "0222", "0223", "0224", "0225", "0226", "0227", "0228"]),
    ("TRANS", "TRANS", ["1709", "1710", "1708"]),
    ("SUPER", "TRANS", ["1665", "0179"]),
    ("MEDIO", "TRANS", ["1664", "0156"]),
    ("MEDIO", "PROY", ["1713"]),
]

# Nº de RA que debe tener cada grupo: si cambia, el parseo del BOE se ha roto.
ESPERADO = {"DAM": 69, "DAW": 68, "ASIR": 72, "SMR": 52,
            "TRANS": 16, "SUPER": 11, "MEDIO": 15}

# Nº de criterios de evaluación de cada grupo (guarda de regresión del parseo).
ESPERADO_CRITERIOS = {"DAM": 539, "DAW": 530, "ASIR": 600, "SMR": 430,
                      "TRANS": 108, "SUPER": 88, "MEDIO": 117}


def descargar(url: str) -> str:
    peticion = urllib.request.Request(
        url, headers={"User-Agent": "Mozilla/5.0 (telosabes2026; datos educativos)"})
    with urllib.request.urlopen(peticion, timeout=60) as respuesta:
        return respuesta.read().decode("utf-8", errors="replace")


def a_texto(documento: str) -> str:
    """Quita etiquetas HTML y deja el texto con un solo espacio entre palabras."""
    texto = re.sub(r"<[^>]+>", " ", documento)
    texto = html.unescape(texto)  # &iacute; -> í, &#225; -> á, &nbsp; -> espacio
    return re.sub(r"\s+", " ", texto).strip()


def titulo_boe(documento: str) -> str:
    coincidencia = re.search(r"<title>(.*?)</title>", documento, re.S)
    if not coincidencia:
        return ""
    return re.sub(r"\s+", " ", html.unescape(coincidencia.group(1))).strip()


def partir_criterios(region: str) -> list:
    """Corta una región en los criterios a), b), c)... de la tabla oficial.

    Solo acepta letras contiguas desde la a): una letra repetida dentro de un
    texto se salta y un salto de letra termina la lista. Así la lista se corta
    sola en el primer texto que viene detrás (el siguiente RA u otro anexo).
    """
    marcas = list(re.finditer(r"(?:^|\s)([a-z])\)\s*", region))
    criterios = []
    esperada = 0  # índice en el alfabeto de la próxima letra: 0 = «a)»
    for i, marca in enumerate(marcas):
        indice = ord(marca.group(1)) - ord("a")
        if indice != esperada:
            if indice < esperada:
                continue  # letra suelta repetida dentro de un texto
            break  # salto de letra: la lista oficial ha terminado
        fin = marcas[i + 1].start() if i + 1 < len(marcas) else len(region)
        texto = region[marca.start():fin].strip()
        criterios.append(re.sub(r"^[a-z]\)\s*", "", texto).strip())
        esperada = indice + 1
    return criterios


def extraer_modulos(texto: str) -> dict:
    """Devuelve {código: [(RA1, [sus criterios]), (RA2, [...]), ...]}."""
    marcas = [m.start() for m in re.finditer(r"M[oó]dulo [Pp]rofesional:", texto)]
    modulos = {}

    for posicion, inicio in enumerate(marcas):
        fin = marcas[posicion + 1] if posicion + 1 < len(marcas) else len(texto)
        anexo = re.search(r"\sANEXO\s+[IVXLC]+\b", texto[inicio:])
        if anexo:
            fin = min(fin, inicio + anexo.start())
        bloque = texto[inicio:fin]

        codigo = re.search(r"Código:\s*([A-Za-z0-9]+)", bloque)
        if not codigo:
            continue  # módulos sin código (optativos) o anexos de otra índole

        inicio_ra = re.search(r"Resultados de aprendizaje", bloque, re.I)
        if not inicio_ra:
            continue
        resto = bloque[inicio_ra.start():]
        resto = re.sub(r"Resultados de aprendizaje y criterios de evaluaci[oó]n\s*[:.]?\s*",
                       "", resto, count=1, flags=re.I)

        parejas = []
        partes = resto.split("Criterios de evaluación")
        if len(partes) > 1:
            # Estructura: "N. RA... Criterios de evaluación: a) ... b) ... N+1. RA..."
            for k, parte in enumerate(partes[:-1]):
                numero = k + 1
                marcas_n = list(re.finditer(r"(?:^|\s)%d\.\s*" % numero, parte))
                if not marcas_n:
                    continue
                texto_ra = re.sub(r"^\s*%d\.\s*" % numero, "",
                                  parte[marcas_n[-1].start():], count=1).strip()
                if len(texto_ra) < 15:
                    continue
                # Los criterios de este RA están en la parte siguiente: desde
                # el principio hasta la marca del RA que viene detrás.
                region = partes[k + 1]
                corte = re.search(r"(?:^|\s)%d\.\s*" % (numero + 1), region)
                if corte:
                    region = region[:corte.start()]
                parejas.append((texto_ra, partir_criterios(region)))
        else:
            # Estructura sin etiqueta: "N. RA... a) criterio ... N+1. RA..."
            puntos = [(int(m.group(1)), m.start()) for m in
                      re.finditer(r"(?:^|\s)(\d{1,2})\.(?=\s|[A-ZÁÉÍÓÚÑ¿\"«])", resto)]
            esperado = 1
            for numero, posicion_ra in puntos:
                if numero != esperado:
                    continue
                siguiente = next((p for p in puntos
                                  if p[1] > posicion_ra and p[0] == esperado + 1), None)
                fin_ra = siguiente[1] if siguiente else len(resto)
                criterio = re.search(r"(?:^|\s)[a-z]\)\s", resto[posicion_ra:fin_ra])
                if criterio:
                    region = resto[posicion_ra + criterio.start():fin_ra]
                    texto_ra = resto[posicion_ra:posicion_ra + criterio.start()]
                else:
                    region = ""
                    texto_ra = resto[posicion_ra:fin_ra]
                texto_ra = re.sub(r"^\s*%d\.\s*" % numero, "", texto_ra, count=1).strip()
                if len(texto_ra) < 15:
                    continue
                parejas.append((texto_ra, partir_criterios(region)))
                esperado += 1

        if parejas:
            modulos[codigo.group(1)] = parejas

    return modulos


def main() -> int:
    documentos = {}
    for clave, url in FUENTES.items():
        documento = descargar(url)
        documentos[clave] = documento
        print(f"Descargado {clave:6s} ({len(documento):>8,} caracteres)  {titulo_boe(documento)}")

    extraidos = {clave: extraer_modulos(a_texto(documento))
                 for clave, documento in documentos.items()}

    lineas_ra = []
    lineas_criterios = []
    fallos = []
    sin_criterios = []
    resumen_ra = {}
    resumen_criterios = {}

    for grupo, fuente, codigos in COBERTURA:
        for codigo in codigos:
            parejas = extraidos[fuente].get(codigo)
            if not parejas:
                fallos.append(f"{fuente} no tiene RA del módulo {codigo}")
                continue
            resumen_ra.setdefault(grupo, 0)
            resumen_criterios.setdefault(grupo, 0)
            for orden, (texto_ra, criterios) in enumerate(parejas, start=1):
                if "|" in texto_ra:
                    fallos.append(f"el RA {orden} de {codigo} contiene «|»")
                    continue
                if len(texto_ra) > 8000:
                    fallos.append(f"el RA {orden} de {codigo} tiene {len(texto_ra)} caracteres")
                lineas_ra.append(f"{grupo}|{codigo}|{orden}|{texto_ra}")
                resumen_ra[grupo] += 1
                if not criterios:
                    sin_criterios.append(f"{grupo} {codigo} RA{orden}")
                for orden_criterio, texto_criterio in enumerate(criterios, start=1):
                    if "|" in texto_criterio:
                        fallos.append(f"el criterio {orden_criterio} de {codigo} RA{orden} "
                                      f"contiene «|»")
                        continue
                    if len(texto_criterio) > 8000:
                        fallos.append(f"el criterio {orden_criterio} de {codigo} RA{orden} "
                                      f"tiene {len(texto_criterio)} caracteres")
                    lineas_criterios.append(
                        f"{grupo}|{codigo}|{orden}|{orden_criterio}|{texto_criterio}")
                    resumen_criterios[grupo] += 1

    print("\nPor grupo          RA           criterios")
    for grupo in ["DAM", "DAW", "ASIR", "SMR", "TRANS", "SUPER", "MEDIO"]:
        ra = resumen_ra.get(grupo, 0)
        criterios = resumen_criterios.get(grupo, 0)
        marca_ra = "ok" if ra == ESPERADO[grupo] else f"¡ESPERABA {ESPERADO[grupo]}!"
        marca_cr = ("ok" if criterios == ESPERADO_CRITERIOS.get(grupo)
                    else f"¡ESPERABA {ESPERADO_CRITERIOS.get(grupo)}!")
        print(f"  {grupo:6s} {ra:>5} {marca_ra:>16} {criterios:>6} {marca_cr:>16}")
        if ra != ESPERADO[grupo]:
            fallos.append(f"el grupo {grupo} tiene {ra} RA y se esperaban {ESPERADO[grupo]}")
        if criterios != ESPERADO_CRITERIOS.get(grupo):
            fallos.append(f"el grupo {grupo} tiene {criterios} criterios y se esperaban "
                          f"{ESPERADO_CRITERIOS.get(grupo)}")

    if sin_criterios:
        fallos.append(f"hay {len(sin_criterios)} RA sin criterios: "
                      + ", ".join(sin_criterios[:10]))

    if fallos:
        print("\nFALLOS:")
        for fallo in fallos:
            print(f"  - {fallo}")
        return 1

    cabecera_ra = [
        "# Resultados de aprendizaje oficiales de los módulos profesionales.",
        "# Fichero generado por tools/generar_ras.py a partir del BOE; no editar a mano.",
        "# Formato de cada línea: GRUPO|CODIGO_MODULO|ORDEN|TEXTO",
        "# Grupos (a qué siglas se aplica cada línea, ver RasIniciales):",
        "#   DAM   Real Decreto 450/2010      -> ciclo DAM",
        "#   DAW   Real Decreto 686/2010      -> ciclo DAW",
        "#   ASIR  Real Decreto 1629/2009     -> ciclo ASIR",
        "#   SMR   Real Decreto 1691/2007     -> ciclo SMR",
        "#   TRANS Real Decreto 659/2023      -> DAM, DAW, ASIR y SMR (1709, 1710, 1708)",
        "#   SUPER Real Decreto 659/2023      -> DAM, DAW y ASIR (1665, 0179)",
        "#   MEDIO Real Decreto 659/2023 y 499/2024 -> SMR (1664, 0156 y 1713)",
        "",
    ]
    cabecera_criterios = [
        "# Criterios de evaluación oficiales de cada resultado de aprendizaje.",
        "# Fichero generado por tools/generar_ras.py a partir del BOE; no editar a mano.",
        "# Formato de cada línea: GRUPO|CODIGO_MODULO|ORDEN_RA|ORDEN|TEXTO",
        "# El ORDEN es la letra de la tabla oficial: 1 = a), 2 = b), 3 = c)...",
        "# Los grupos son los mismos que en ras.txt (ver RasIniciales).",
        "",
    ]
    SALIDA_RA.write_text("\n".join(cabecera_ra + lineas_ra) + "\n", encoding="utf-8")
    SALIDA_CRITERIOS.write_text("\n".join(cabecera_criterios + lineas_criterios) + "\n",
                                encoding="utf-8")
    print(f"\nEscrito {SALIDA_RA.name} con {len(lineas_ra)} RA y "
          f"{SALIDA_CRITERIOS.name} con {len(lineas_criterios)} criterios.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
