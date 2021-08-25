package uce.proyect.service.agreementImp;

import lombok.AllArgsConstructor;
import org.json.JSONObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import uce.proyect.service.agreement.EmailService;

import javax.mail.MessagingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@AllArgsConstructor
public class EmailServiceImp implements EmailService {

    private JavaMailSender javaMailSender;

    @Override
    public String enviarEmail() { // Metodo de email de prueba
        var mailMessage = new SimpleMailMessage();

        mailMessage.setFrom("sgvuce@gmail.com");
        mailMessage.setTo("sgvuce@gmail.com");
        mailMessage.setSubject("Vacunación");
        mailMessage.setText("Mensaje con tilde en Díaz");

        this.javaMailSender.send(mailMessage);

        return "Todo nice";
    }

    @Override
    public void enviarEmail(String destinatario, LocalDate fechaInicio, LocalDate fechaFinal, String facultad) { // Envio de las fechas del plan de vacunacion

        var mailMessage = new SimpleMailMessage();

        mailMessage.setFrom("sgvuce@gmail.com");
        mailMessage.setTo(destinatario);
        mailMessage.setSubject("Calendario Vacunación");
        mailMessage.setText(
                "Tenga un cordial saludo, le informamos el día de su vacunación"
                        .concat("\n\nFecha Inicio: ").concat(fechaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .concat("\n\nFecha Final: ").concat(fechaFinal.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .concat("\n\nLugar Vacunación: ".concat(facultad))
                        .concat("\n\n\nLos horarios de atención serán de 08:00 AM hasta 16:00 PM. Tome las devidas precauciones.")
                        .concat("\n\n\n\n\n\n")
                        .concat("No responder a este mensaje."));
        this.javaMailSender.send(mailMessage);
    }

    // Metodo que permite enviar archivo adjuntos en los email
    @Override
    public JSONObject enviarComprobante(JSONObject recursos) throws MessagingException {

        var carnet = (byte[]) recursos.get("recurso"); // tomo el pdf en bytes que genero el estudiante para enviarlo
        var mailDestinatario = recursos.get("mailDestinatario").toString(); // el destinatario igual

        var mimeMessage = this.javaMailSender.createMimeMessage();
        var mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        mimeMessageHelper.setFrom("sgvuce@gmail.com"); // cambiar destinatario a emisor, solo es para desarrollo
        mimeMessageHelper.setTo(mailDestinatario);
        mimeMessageHelper.setSubject("Carnet Vacunación");
        mimeMessageHelper.setText("<h4>Buena día. Adjuntamos su carnet de Vacunación contra la COVID-19.</h4>", true); // Se puede enviar html

        mimeMessageHelper.addAttachment("carnetVacunacion.pdf", new ByteArrayResource(carnet)); // Se envia la ruta definina dentro de resources statics img

        this.javaMailSender.send(mimeMessage);

        var respuesta = new JSONObject();
        respuesta.put("destinatario", mailDestinatario);
        respuesta.put("estado", "enviado");

        respuesta.put("fecha_emision", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        return respuesta;
    }

    @Override
    public void enviarEmailCredenciales(String email, String nombreUsuario, String password) {
        var simpleMailMessage = new SimpleMailMessage();

        simpleMailMessage.setFrom("sgvuce@gmail.com");
        simpleMailMessage.setTo(email);
        simpleMailMessage.setSubject("Credenciales S.G.V UCE");
        simpleMailMessage.setText(
                "Buen día, sus credenciales para el ingreso a la plataforma S.G.V UCE son \n"
                        .concat("Nombre de usuario: ").concat(nombreUsuario)
                        .concat("\nCotraseña: ").concat(password)
                        .concat("\n\n\nGuardelas correctamente, pues no podrán ser cambiadas.")
        );

        this.javaMailSender.send(simpleMailMessage);
    }
}
