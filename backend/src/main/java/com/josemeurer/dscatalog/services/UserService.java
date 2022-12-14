package com.josemeurer.dscatalog.services;

import com.josemeurer.dscatalog.dto.UserDTO;
import com.josemeurer.dscatalog.dto.UserInsertDTO;
import com.josemeurer.dscatalog.dto.UserUpdateDTO;
import com.josemeurer.dscatalog.entities.User;
import com.josemeurer.dscatalog.repositories.RoleRepository;
import com.josemeurer.dscatalog.repositories.UserRepository;
import com.josemeurer.dscatalog.services.exceptions.DatabaseException;
import com.josemeurer.dscatalog.services.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

	private static Logger logger = LoggerFactory.getLogger(UserService.class); //slf4j

	@Autowired
	private BCryptPasswordEncoder bCrypt;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private RoleRepository roleRepository;

	@Transactional(readOnly = true)
	public Page<UserDTO> findAllPaged(Pageable pageable) {
		Page<User> page = userRepository.findAll(pageable);
		return page.map(x -> new UserDTO(x));
	}
	
	@Transactional(readOnly = true)
	public UserDTO findById(Long id) {
		Optional<User> obj = userRepository.findById(id);
		User entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		return new UserDTO(entity);
	}
	
	@Transactional
	public UserDTO insert(UserInsertDTO dto) {
		User entity = new User();
		dtoToEntity(dto, entity);
		entity.setPassword(bCrypt.encode(dto.getPassword())); //Transaforma a senha em hash;
		entity = userRepository.save(entity);
		return new UserDTO(entity);
	}
	
	@Transactional
	public UserDTO update(Long id, UserUpdateDTO dto) {
		try {
			User entity = userRepository.getOne(id);
			dtoToEntity(dto, entity);
			entity = userRepository.save(entity);
			return new UserDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id not found " + id);
		}
	}
	
	public void delete(Long id) {
		
		try {
			userRepository.deleteById(id);
		}
		catch (EmptyResultDataAccessException e) {
			throw new ResourceNotFoundException("Id not found " + id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
	}
	
	private void dtoToEntity(UserDTO dto, User entity) {
		entity.setFirstName(dto.getFirstName());
		entity.setLastName(dto.getLastName());
		entity.setEmail(dto.getEmail());
		
		entity.getRoles().clear();
		dto.getRoles().forEach(x -> entity.getRoles()
				.add(roleRepository.getOne(x.getId())));
	}

	@Override //Spring Security
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		User entity = userRepository.findByEmail(username);
		if (entity == null) {
			logger.error("User not found: " + username);
			throw new UsernameNotFoundException("Email not found");
		}
		logger.info("User found: " + username);
		return entity;
	}
}
