import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilidad para generar el hash BCrypt de una contraseña.
 * Se usa desde db/crear-admin.ps1, pero también se puede ejecutar a mano:
 *
 *   java -cp "ruta\spring-security-crypto-x.y.z.jar" db/GenerarHash.java "MiClave"
 *
 * El resultado es lo que se guarda en la columna usuarios.clave.
 */
public class GenerarHash {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java -cp <spring-security-crypto.jar> GenerarHash.java <contraseña>");
            System.exit(1);
        }
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
