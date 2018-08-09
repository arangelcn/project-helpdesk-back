package com.arcn.helpdesk.api.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import com.arcn.helpdesk.api.response.Response;
import com.arcn.helpdesk.api.security.entity.User;
import com.arcn.helpdesk.api.service.UserService;

@RestController              // Elemento ResFull
@RequestMapping("/api/user") // Mapeamento para requisição http
@CrossOrigin(origins = "*")  // Libera entrada de qualquer porta / servidor
public class UserController {
	
        // Injeções de dependências da classe de servico 
	@Autowired
	private UserService userService;
	
        // Injeções de dependências da classe da encriptação de senhas 
	@Autowired
	private PasswordEncoder passwordEncoder;

        // Classe responsável por salvar um usuário
        /*
            Objeto Response é o responsável por gerenciar a comunicação Angular <-----> Spring
        */
        @PostMapping                                // Chamado na requisição /api/user
        @PreAuthorize("hasAnyRole('ADMIN')")        // Somente para administradores 
	public ResponseEntity<Response<User>> create (HttpServletRequest request,
                                                      @RequestBody User user, 
                                                      BindingResult result) {
            Response<User> response = new Response<User>();
            try {
                validateCreateUser(user, result); // Validação do e-mail 
                if (result.hasErrors()) { 
                    // Para cada erro forEach() ele é adicionado na lista de Erros do objeto Response 
                    result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
                    return ResponseEntity.badRequest().body(response);
                }
                user.setPassword(passwordEncoder.encode(user.getPassword())); // Encripta a senha
                User userPersisted = (User) userService.createOrUpdate(user); // Cria um novo usuario para persistencia 
                response.setData(userPersisted);                              // Seta o usuario novo para o sistema front
            } catch (DuplicateKeyException duplicatedExeption) {
                response.getErrors().add("E-mail já existe");
                return ResponseEntity.badRequest().body(response);
            } catch (Exception e) { 
                response.getErrors().add(e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        }
        
        // Valida se é usuario valido com base no E-mail para Criacao 
        private void validateCreateUser(User user, BindingResult result) {
            if (user.getEmail() == null) { 
                result.addError(new ObjectError("User", "E-mail inválido"));
            }
        }
        
        @PutMapping  // Referencia com o update do front POST/GET
        @PreAuthorize("hasAnyRole('ADMIN')") 
        public ResponseEntity<Response<User>> update(HttpServletRequest request,
                                                      @RequestBody User user, 
                                                      BindingResult result) { 
            Response<User> response = new Response<User>();
            try {
                validateUpdateUser(user, result);
                if (result.hasErrors()) { 
                    // Para cada erro forEach() ele é adicionado na lista de Erros do objeto Response 
                    result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
                    return ResponseEntity.badRequest().body(response); // Retorna um corpo com os erros 
                }
                user.setPassword(passwordEncoder.encode(user.getPassword())); // Encripta a senha
                User userPersisted = (User) userService.createOrUpdate(user); // Cria um novo usuario para persistencia 
                response.setData(userPersisted);                              // Seta o usuario novo para o sistema front
            } catch (Exception e) {
                response.getErrors().add(e.getMessage()); 
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        }  
        
        // Valida se é usuario valido com base no E-mail para Update 
        private void validateUpdateUser(User user, BindingResult result) {
            if (user.getId() == null) { 
                result.addError(new ObjectError("User", "Id inválido "));
            }
            if (user.getEmail() == null) { 
                result.addError(new ObjectError("User", "E-mail inválido "));
            }
        }
        
        // Encontra um usuario pelo ID
        @GetMapping(value = "{id}")
        @PreAuthorize("hasAnyRole('ADMIN')") 
        public ResponseEntity<Response<User>> findById(@PathVariable("id") String id) { 
            Response<User> response = new Response<User>();
            User user = userService.findById(id); 
            if (user == null) { 
                response.getErrors().add("Não foi localizado usuário com essa id");
                return ResponseEntity.badRequest().body(response);
            }
            response.setData(user);
            return ResponseEntity.ok(response);
        }
        
        // Deleta um usuario
        @DeleteMapping(value = "{id}")
        @PreAuthorize("hasAnyRole('ADMIN')") 
        public ResponseEntity<Response<String>> delete(@PathVariable("id") String id) {
            Response<String> response = new Response<String>();
            User user = (User) userService.findById(id);
            
            if (user == null) { 
                response.getErrors().add("Não foi localizado usuário com essa id");
                return ResponseEntity.badRequest().body(response);
            }
            
            userService.delete(id);
            return ResponseEntity.ok(new Response<String>());
        }
        
        // Busca todos os usuarios com base em pagina e qtd
        @GetMapping(value = "{page}/{count}")
        @PreAuthorize("hasAnyRole('ADMIN')") 
        public ResponseEntity<Response<Page<User>>> findAll( @PathVariable int page, 
                @PathVariable int count) { 
                
            Response<Page<User>> response = new Response<Page<User>>();
            Page<User> users = userService.findAll(page, count);
            
            response.setData(users);
            
            return ResponseEntity.ok(response);
        }
	
}
