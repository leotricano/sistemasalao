package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.model.Cliente;
import br.com.leotricano.sistemasalao.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
//Service = o tratamento dos dados.
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Cliente salvar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }
}