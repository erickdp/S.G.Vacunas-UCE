package uce.proyect.service.agreement;

import org.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;
import uce.proyect.exceptions.NoEncontradorException;

import java.util.Collection;

// Se deinfe le contrato con las transacciones arriba de los metodos para evitar codigo repetitivo
public interface CoreService<T> {

    @Transactional
    T agregarOActualizar(T pojo);

    @Transactional(readOnly = true)
    Collection<T> listar() throws NoEncontradorException; // Igual la excepcion, no es necesario

    @Transactional(readOnly = true)
    T buscarPorId(String identificador);

    @Transactional
    JSONObject eliminar(String identificador);
}
