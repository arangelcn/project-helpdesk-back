package com.arcn.helpdesk.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.arcn.helpdesk.api.security.entity.User;

// Interfaçe do Spring Data MongoDB para implementar metodos para a conexao com o banco 
                                       //MongoRepository <Documento, Tipo Id> 
public interface UserRepository extends MongoRepository<User, String> {

	User findByEmail(String email);

}
