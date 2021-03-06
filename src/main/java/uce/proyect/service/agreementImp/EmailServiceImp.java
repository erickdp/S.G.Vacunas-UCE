package uce.proyect.service.agreementImp;

import freemarker.template.TemplateException;
import lombok.AllArgsConstructor;
import org.json.JSONObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import uce.proyect.service.agreement.EmailService;

import javax.mail.MessagingException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Service
@AllArgsConstructor
public class EmailServiceImp implements EmailService {

    private JavaMailSender javaMailSender;

    private FreeMarkerConfigurer freeMarkerConfigurer;

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
    public void enviarEmailPlan(String destinatario, LocalDate fechaInicio, LocalDate fechaFinal, String centroVacunacion, String fase) throws MessagingException, IOException, TemplateException { // Envio de las fechas del plan de vacunacion

        var mimeMessage = this.javaMailSender.createMimeMessage();
        var mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

        mimeMessageHelper.setFrom("sgvuce@gmail.com");
        mimeMessageHelper.setTo(destinatario);
        mimeMessageHelper.setSubject("Calendario Vacunación");

        var stringObjectHashMap = new HashMap<String, Object>();
        stringObjectHashMap.put("centroVacunacion", centroVacunacion);
        stringObjectHashMap.put("fechaInicio", fechaInicio.toString());
        stringObjectHashMap.put("fechaFinal", fechaFinal.toString());
        stringObjectHashMap.put("fase", fase);

        var template = freeMarkerConfigurer.getConfiguration().getTemplate("/email-template-calendario.ftlh");
        var templatePreparado = FreeMarkerTemplateUtils.processTemplateIntoString(template, stringObjectHashMap);

        mimeMessageHelper.setText(
                templatePreparado,
                true);
        this.javaMailSender.send(mimeMessage);
    }

    // Metodo que permite enviar archivo adjuntos en los email
    @Override
    public JSONObject enviarComprobante(JSONObject recursos) throws MessagingException, IOException, TemplateException {

        var carnet = (byte[]) recursos.get("recurso"); // tomo el pdf en bytes que genero el estudiante para enviarlo
        var mailDestinatario = recursos.get("mailDestinatario").toString(); // el destinatario igual

        var mimeMessage = this.javaMailSender.createMimeMessage();
        var mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        mimeMessageHelper.setFrom("sgvuce@gmail.com"); // cambiar destinatario a emisor, solo es para desarrollo
        mimeMessageHelper.setTo(mailDestinatario);
        mimeMessageHelper.setSubject("Carnet Vacunación");

        var stringObjectHashMap = new HashMap<String, Object>();

        var template = freeMarkerConfigurer.getConfiguration().getTemplate("/email-template-certificado.ftlh");
        var templatePreparado = FreeMarkerTemplateUtils.processTemplateIntoString(template, stringObjectHashMap);
        mimeMessageHelper.setText(
                templatePreparado,
                true);

        mimeMessageHelper.addAttachment("carnet-vacunacion.pdf", new ByteArrayResource(carnet)); // Se envia la ruta definina dentro de resources statics img

        this.javaMailSender.send(mimeMessage);

        var respuesta = new JSONObject();
        respuesta.put("destinatario", mailDestinatario);
        respuesta.put("estado", "enviado");

        respuesta.put("fecha_emision", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        return respuesta;
    }

    @Override
    public void enviarEmailCredenciales(String email, String nombreUsuario, String password) throws MessagingException, IOException, TemplateException {
        var mimeMessage = this.javaMailSender.createMimeMessage();
        var mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

        mimeMessageHelper.setFrom("sgvuce@gmail.com");
        mimeMessageHelper.setTo(email);
        mimeMessageHelper.setSubject("Credenciales S.G.V UCE");

        var stringObjectHashMap = new HashMap<String, Object>();
        stringObjectHashMap.put("nombreUsuario", nombreUsuario);
        stringObjectHashMap.put("password", password);

        var template = freeMarkerConfigurer.getConfiguration().getTemplate("/email-template.ftlh");
        var templatePreparado = FreeMarkerTemplateUtils.processTemplateIntoString(template, stringObjectHashMap);
        mimeMessageHelper.setText(
                templatePreparado.toString(), true
        );

        this.javaMailSender.send(mimeMessage);
    }
}
