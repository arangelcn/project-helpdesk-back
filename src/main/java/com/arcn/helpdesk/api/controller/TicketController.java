package com.arcn.helpdesk.api.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arcn.helpdesk.api.dto.Summary;
import com.arcn.helpdesk.api.response.Response;
import com.arcn.helpdesk.api.security.entity.ChangeStatus;
import com.arcn.helpdesk.api.security.entity.Ticket;
import com.arcn.helpdesk.api.security.entity.User;
import com.arcn.helpdesk.api.security.enums.ProfileEnum;
import com.arcn.helpdesk.api.security.enums.StatusEnum;
import com.arcn.helpdesk.api.security.jwt.JwtTokenUtil;
import com.arcn.helpdesk.api.service.TicketService;
import com.arcn.helpdesk.api.service.UserService;

@RestController
@RequestMapping("/api/ticket")    // Mapeamento para Front
@CrossOrigin(origins = "*")       // Servidores de Origem - HOST + PORTA
public class TicketController {

    // Importando dependecias do TicketService 
    @Autowired
    private TicketService ticketService;

    // Importando dependencias do Token 
    @Autowired
    protected JwtTokenUtil jwtTokenUtil;

    // Importando dependencias do UserService 
    @Autowired
    private UserService userService;

    // Método para criação do Ticket
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<Response<Ticket>> create(
            HttpServletRequest request, // reuisicao Http
            @RequestBody Ticket ticket, // Corpo da Requisição 
            BindingResult result) {  // Resultado 

        Response<Ticket> response = new Response<Ticket>();
        try {
            validateCreateTicket(ticket, result);
            if (result.hasErrors()) {
                result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(response);
            }
            ticket.setStatus(StatusEnum.getStatus("New"));
            ticket.setUser(userFromRequest(request));
            ticket.setDate(new Date());
            ticket.setNumber(generateNumber());
            Ticket ticketPersisted = (Ticket) ticketService.createOrUpdate(ticket);
            response.setData(ticketPersisted);
        } catch (Exception e) {
            response.getErrors().add(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    // Metodo para validacao do Ticket 
    private void validateCreateTicket(Ticket ticket, BindingResult result) {
        if (ticket.getTitle() == null) {
            result.addError(new ObjectError("Ticket", "Ticket sem título"));
            return;
        }
    }

    // Metodo para verificar o usuario logado 
    public User userFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        String email = jwtTokenUtil.getUsernameFromToken(token);
        return userService.findByEmail(email);
    }

    // Criação do Número do Ticket:
    private Integer generateNumber() {
        Random random = new Random();
        return random.nextInt(9999);
    }

    // Método para alteração do Ticket
    @PutMapping
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<Response<Ticket>> update(
            HttpServletRequest request, // reuisicao Http
            @RequestBody Ticket ticket, // Corpo da Requisição 
            BindingResult result) {  // Resultado 

        Response<Ticket> response = new Response<Ticket>();
        try {
            validateUpdateTicket(ticket, result);
            validateCreateTicket(ticket, result);
            if (result.hasErrors()) {
                result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(response);
            }
            Ticket ticketCurrent = ticketService.findById(ticket.getId());
            ticket.setStatus(ticketCurrent.getStatus());
            ticket.setUser(ticketCurrent.getUser());
            ticket.setDate(ticketCurrent.getDate());
            ticket.setNumber(ticketCurrent.getNumber());
            if (ticketCurrent.getAssignedUser() != null) {
                ticket.setAssignedUser(ticketCurrent.getAssignedUser());
            }
            Ticket ticketPersisted = (Ticket) ticketService.createOrUpdate(ticket);
            response.setData(ticketPersisted);
        } catch (Exception e) {
            response.getErrors().add(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    // Metodo para validacao do Ticket para update
    private void validateUpdateTicket(Ticket ticket, BindingResult result) {
        if (ticket.getId() == null) {
            result.addError(new ObjectError("Ticket", "Ticket sem Id"));
            return;
        }
        if (ticket.getTitle() == null) {
            result.addError(new ObjectError("Ticket", "Ticket sem título"));
            return;
        }
    }

    // Método para pesquida do Ticket pelo Id
    @GetMapping(value = "{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','TECHNICIAN')")
    public ResponseEntity<Response<Ticket>> findById(@PathVariable("id") String id) {

        Response<Ticket> response = new Response<Ticket>();
        Ticket ticket = ticketService.findById(id);
        if (ticket == null) {
            response.getErrors().add("Registro não encontrado: " + id);
            return ResponseEntity.badRequest().body(response);
        }

        List<ChangeStatus> changes = new ArrayList<ChangeStatus>();
        Iterable<ChangeStatus> changesCurrent = ticketService.listChangeStatus(ticket.getId());
        for (Iterator<ChangeStatus> iterator = changes.iterator(); iterator.hasNext();) {
            ChangeStatus next = iterator.next();
            changes.add(next);
        }
        ticket.setChanges(changes);
        response.setData(ticket);
        return ResponseEntity.ok(response);
    }

    // Método para deletar Ticket pelo Id
    @DeleteMapping(value = "{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','TECHNICIAN')")
    public ResponseEntity<Response<String>> delete(@PathVariable("id") String id) {
        Response<String> response = new Response<String>();
        Ticket ticket = ticketService.findById(id);
        if (ticket == null) {
            response.getErrors().add("Registro não encontrado: " + id);
            return ResponseEntity.badRequest().body(response);
        }
        ticketService.delete(id);
        return ResponseEntity.ok(new Response<String>());
    }

    // Metodo findAll 
    @GetMapping(value = "{page}/{count}")
    @PreAuthorize("hasAnyRole('CUSTOMER','TECHNICIAN')")
    public ResponseEntity<Response<Page<Ticket>>> findAll(
            HttpServletRequest request,
            @PathVariable("page") int page,
            @PathVariable("count") int count) {

        Response<Page<Ticket>> response = new Response<Page<Ticket>>();
        Page<Ticket> tickets = null;
        User userRequest = userFromRequest(request);
        if (userRequest.getProfile().equals(ProfileEnum.ROLE_TECHNICIAN)) {
            tickets = ticketService.listTicket(page, count);
        } else if (userRequest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {
            tickets = ticketService.findByCurrentUser(page, count, userRequest.getId());
        }
        response.setData(tickets);
        return ResponseEntity.ok(response);
    }

    // Metodo findAll 
    @GetMapping(value = "{page}/{count}/{number}/{title}/{status}/{priority}/{assigned}")
    @PreAuthorize("hasAnyRole('CUSTOMER','TECHNICIAN')")
    public ResponseEntity<Response<Page<Ticket>>> findByParams(
            HttpServletRequest request,
            @PathVariable int page,
            @PathVariable int count,
            @PathVariable Integer number,
            @PathVariable String title,
            @PathVariable String status,
            @PathVariable String priority,
            @PathVariable boolean assigned) {

        // Fazendo a validação dos parâmetros: 
        title = title.equals("uninformed") ? "" : title;
        status = status.equals("uninformed") ? "" : status;
        priority = priority.equals("uninformed") ? "" : priority;

        //
        Response<Page<Ticket>> response = new Response<Page<Ticket>>();
        Page<Ticket> tickets = null;

        if (number > 0) {
            tickets = ticketService.findByNumber(page, count, number);
        } else {
            User userReuest = userFromRequest(request);
            if (userReuest.getProfile().equals(ProfileEnum.ROLE_TECHNICIAN)) {
                if (assigned) {
                    tickets = ticketService.findByParametersAndAssignedUser(page, count, title, status, priority, userReuest.getId());
                } else {
                    tickets = ticketService.findByParameters(page, count, title, status, priority);
                }
            } else if (userReuest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {
                tickets = ticketService.findByParametersAndCurrentUser(page, count, title, status, priority, userReuest.getId());
            }
        }
        response.setData(tickets);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "{id}/{status}")
    @PreAuthorize("hasAnyRole('CUSTOMER','TECHNICIAN')")
    public ResponseEntity<Response<Ticket>> changeStatus(
            @PathVariable("id") String id,
            @PathVariable("status") String status,
            HttpServletRequest request,
            @RequestBody Ticket ticket,
            BindingResult result
    ) {

        Response<Ticket> response = new Response<Ticket>();
        try {
            // Valida se o ticket é válido 
            validateChangeStatus(id, status, result);
            if (result.hasErrors()) {
                // Retorna o erro se houver
                result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(response);
            }
            Ticket ticketCurrent = ticketService.findById(id);
            ticketCurrent.setStatus(StatusEnum.getStatus(status));
            if (status.equals("Assigned")) {
                ticketCurrent.setAssignedUser(userFromRequest(request));
            }
            Ticket ticketPersisted = (Ticket) ticketService.createOrUpdate(ticketCurrent);
            ChangeStatus changeStatus = new ChangeStatus();
            changeStatus.setUserChange(userFromRequest(request));
            changeStatus.setDateChangeStatus(new Date());
            changeStatus.setStatus(StatusEnum.getStatus(status));
            changeStatus.setTicket(ticketPersisted);
            ticketService.createChangeStatus(changeStatus);
            response.setData(ticketPersisted);
        } catch (Exception e) {
            response.getErrors().add(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    // Metodo para validacao do Ticket para mudar status:
    private void validateChangeStatus(String id, String status, BindingResult result) {
        if (id == null || id.equals("")) {
            result.addError(new ObjectError("Ticket", "Ticket sem Id"));
            return;
        }
        if (status == null || status.equals("")) {
            result.addError(new ObjectError("Ticket", "Status inválido"));
            return;
        }
    }
    
    // Metodo que garante o resumo dos chamados 
    @GetMapping(value = "/summary")
    public ResponseEntity<Response<Summary>> findChart() {
        Response<Summary> response = new Response<Summary>();
        Summary chart = new Summary();
        int amountNew = 0;
        int amountResolved = 0;
        int amountApproved = 0;
        int amountDisapproved = 0;
        int amountAssigned = 0;
        int amountClosed = 0;
        Iterable<Ticket> tickets = ticketService.findAll();
        if (tickets != null) {
            for (Iterator<Ticket> iterator = tickets.iterator(); iterator.hasNext();) {
                Ticket ticket = iterator.next();
                if (ticket.getStatus().equals(StatusEnum.New)) {
                    amountNew++;
                }
                if (ticket.getStatus().equals(StatusEnum.Resolved)) {
                    amountResolved++;
                }
                if (ticket.getStatus().equals(StatusEnum.Approved)) {
                    amountApproved++;
                }
                if (ticket.getStatus().equals(StatusEnum.Disapproved)) {
                    amountDisapproved++;
                }
                if (ticket.getStatus().equals(StatusEnum.Assigned)) {
                    amountAssigned++;
                }
                if (ticket.getStatus().equals(StatusEnum.Closed)) {
                    amountClosed++;
                }
            }
        }
        chart.setAmountNew(amountNew);
        chart.setAmountResolved(amountResolved);
        chart.setAmountApproved(amountApproved);
        chart.setAmountDisapproved(amountDisapproved);
        chart.setAmountAssigned(amountAssigned);
        chart.setAmountClosed(amountClosed);
        response.setData(chart);
        return ResponseEntity.ok(response);
    }

}
