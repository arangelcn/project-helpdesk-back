package com.arcn.helpdesk.api.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.arcn.helpdesk.api.security.entity.ChangeStatus;
import com.arcn.helpdesk.api.security.entity.Ticket;

@Component
public interface TicketService {
        
        // Criar ou Modificar um ticket 
	Ticket createOrUpdate(Ticket ticket);
	
        // Procurar um ticket pedo ID
	Ticket findById(String id);
	
        // Deletar um ticket
	void delete(String id);
	
        // Paginação de um ticket, com o indice da pagina e a quantidade de registros 
	Page<Ticket> listTicket(int page, int count);
	
        // Método para guardar as alterações do Status do ticket
	ChangeStatus createChangeStatus(ChangeStatus changeStatus);
	
        // Retorna uma lista das alterações feitas
	Iterable<ChangeStatus> listChangeStatus(String ticketId);
	
        // Retorna os tickets pelo usuario logado (para clientes)
	Page<Ticket> findByCurrentUser(int page, int count, String userId);
	
        // Procurar pela pagina, o titulo, o status e prioridade
	Page<Ticket> findByParameters(int page, int count,String title, String status,String priority);
	
        // Procura pela pag, titulo, status e prioridade de um único usuario
	Page<Ticket> findByParametersAndCurrentUser(int page, int count, String title,String status,String priority,String userId);
	
        // Procurar pelo numero do ticket
	Page<Ticket> findByNumber(int page, int count,Integer number);
	
        // Procurar todos os tickets 
	Iterable<Ticket> findAll();
	
        // Procurar pelo Usuario designado ao ticket:
	public Page<Ticket> findByParametersAndAssignedUser(int page, int count,String title,String status,String priority,String assignedUserId);
}
