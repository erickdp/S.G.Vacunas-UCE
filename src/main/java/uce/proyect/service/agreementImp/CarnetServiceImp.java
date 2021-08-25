package uce.proyect.service.agreementImp;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uce.proyect.exceptions.CarnetException;
import uce.proyect.exceptions.NoEncontradorException;
import uce.proyect.models.Carnet;
import uce.proyect.repositories.CarnetRepository;
import uce.proyect.repositories.EstudianteRepository;
import uce.proyect.service.agreement.CarnetService;
import uce.proyect.service.agreement.EstudianteService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
@Slf4j
public class CarnetServiceImp implements CarnetService {

    private CarnetRepository carnetRepository;

    private EstudianteRepository estudianteRepository;

    private EstudianteService estudianteService;

    @Override
    public Carnet agregarOActualizar(Carnet pojo) {
        return this.carnetRepository.save(pojo);
    }

    @Override
    public Collection<Carnet> listar() throws RuntimeException {
        var list = this.carnetRepository.findAll();
        if (list.isEmpty()) {
            throw new RuntimeException("Sin registros");
        }
        return list;
    }

    @Override
    public Carnet buscarPorId(String identificador) {
        var carnet = this.carnetRepository.findByEstudiante(identificador);
        return carnet.orElseThrow();
    }

    @Override
    public JSONObject eliminar(String identificador) {
        var carnet = this.carnetRepository.findById(identificador);
        var jsonObject = new JSONObject();
        if (carnet.isPresent()) {
            this.carnetRepository.delete(carnet.get());
            jsonObject.put("Eliminado_C", "Se ha eliminado el carnet: "
                    .concat(carnet.get().get_id()));
        }
        return jsonObject;
    }

    //    Servicio que valida que el cerdo ya tenga las 2 dosis
    @Override
    @Transactional(readOnly = true)
    public Carnet buscarCarnetPorEstudiante(String estudiante) throws NoEncontradorException {
        var carnetOptional = this.carnetRepository.findByEstudiante(estudiante);
        if (carnetOptional.isPresent()) {
            var carnet = carnetOptional.get();
            if (!carnet.isSegundaDosis()) {
                var fechaPrimeraDosis = carnet.getFechaPrimeraDosis();
                if (fechaPrimeraDosis != null) {
                    var fechaEstimadaSegundaDosis = fechaPrimeraDosis.plusDays(28L);
                    throw new CarnetException(
                            "No se ha suministrado la segunda dosis aún.",
                            fechaPrimeraDosis,
                            fechaEstimadaSegundaDosis,
                            carnet.getNombreVacuna());
                } else {
                    throw new CarnetException("No se ha asigando aún un calendario de Vacunación.");
                }
            }
            return carnet;
        }
        throw new NoEncontradorException("No se ha encontrado ningun carnet para :".concat(estudiante));
    }

    @Override
    public JSONObject generarPdfEnBytes(String estudiante) throws IOException, JRException, NoSuchElementException {

        var data = this.buscarCarnetPorEstudiante(estudiante); // Cargo los datos del carnet y estudiante (vacunado), verifico que tenga las 2 dosis y que exista
        var estu = this.estudianteRepository.findByUsuario(estudiante).orElseThrow(); // Con la validacion anterior ya se define la existencia o no del usuario

        var resource = new ClassPathResource("carnet.jrxml").getInputStream(); // Habia un error al hacer referencia a la ruta absoluta del pdf al usar heroku - RESUELTO

        var dataJson = new JSONObject();

        dataJson.put("centroVacunacion", data.getCentroVacunacion());
        dataJson.put("estudiante", this.estudianteService.nombres(estudiante));
        dataJson.put("cedula", estu.getCedula());
        dataJson.put("fechaNacimiento", LocalDate.now().getYear() - estu.getFechaNacimiento().getYear()); // Solo es hilar mas fino
        dataJson.put("nombreVacuna", data.getNombreVacuna());
        dataJson.put("fechaPrimeraDosis", data.getFechaPrimeraDosis().toString());
        dataJson.put("fechaSegundasDosis", data.getFechaSegundasDosis().toString());
        dataJson.put("vacunadorPrimeraDosis", this.estudianteService.nombres(data.getVacunadorPrimeraDosis()));
        dataJson.put("vacunadorSegundaDosis", this.estudianteService.nombres(data.getVacunadorSegundaDosis()));
        dataJson.put("primeraDosis", (data.isPrimeraDosis()) ? "Sí" : "No");
        dataJson.put("segundaDosis", (data.isSegundaDosis()) ? "Sí" : "No");
        dataJson.put("loteDosisUno", data.getLoteDosisUno());
        dataJson.put("loteDosisDos", data.getLoteDosisDos());

        var jsonDataStream = new ByteArrayInputStream(dataJson.toString().getBytes());
        var ds = new JsonDataSource(jsonDataStream);
        JasperReport jasperReport = JasperCompileManager.compileReport(resource); // Mando a compilar el reporte que está en la ruta resources
//        var dataSource = new JRBeanCollectionDataSource(Collections.singletonList(test)); // Cargo los datos que voy a llenar en el reporte en forma de colección
        Map<String, Object> map = new HashMap<>();
        map.put("createdBy", "sgvacunas"); //
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, map, ds); // Lleno el reporte que compilé con los datos que cague en la colección
//        JasperExportManager.exportReportToPdfFile(jasperPrint, "C:\\Users\\alpal\\Desktop\\carnet.pdf"); // Genera el PDF Físico en una ruta (Se sobreescribe) podrías usar esta línea para mandar por mail solo lo guardar en una ruta del proyecto y cada vez que lo pidan solo se va a sobreescribir (no debe estar abierto el pdf sino genera error al sobreescribir)
        var bytes = JasperExportManager.exportReportToPdf(jasperPrint);// Exporto mi pdf en una cadena de bytes - ERICK: Uso este mismo metodo para no guardar datos en otro lugar que no sea la DB
//         ERICK: Para no acoplar el servicio de mail aqui envio los recursos necesarios para tratarlo desde el controller
        var jsonObject = new JSONObject();
        jsonObject.put("recurso", bytes);
        jsonObject.put("mailDestinatario", estu.getCorreo());
        return jsonObject;
    }
}
