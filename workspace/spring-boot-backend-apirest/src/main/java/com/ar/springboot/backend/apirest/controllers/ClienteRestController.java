package com.ar.springboot.backend.apirest.controllers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ar.springboot.backend.apirest.models.entity.Cliente;
import com.ar.springboot.backend.apirest.models.services.IClienteService;

//CORS, permitir al domino de Angular acceder a todos los metodos del servicio REST.
@CrossOrigin(origins= {"http://localhost:4200"})
@RestController
@RequestMapping("/api") //endpoint
public class ClienteRestController {
	
	//Busca el primer candidato, una clase concreta que implemente la intefaz IClienteService (en este clase la clase es ClienteServiceImpl)
	//En caso de existir dos clases que implmenten la interfaz IClienteService, usar un Qualifier
	@Autowired
	private IClienteService clienteService;
	
	@GetMapping("/clientes")
	public List<Cliente> index(){
		return clienteService.findAll();
	}
	
	@GetMapping("/clientes/page/{page}")
	public Page<Cliente> index(@PathVariable Integer page){
		Pageable pageable=PageRequest.of(page, 4);//Por pagina 4 registros
		return clienteService.findAll(pageable); 
	}

//	@GetMapping("/clientes/{id}")
//	//@ResponseStatus(HttpStatus.OK)//por defecto valor 200
//	public Cliente show(@PathVariable Long id) {		
//		return clienteService.findById(id);
//	}

	//ResponseEntity, permite manejar toda la respuesta Http incluyendo cuerpo, cabecera y codigo de respuesta.
	@GetMapping("/clientes/{id}")
	public ResponseEntity<?> show(@PathVariable Long id) {
		//Mapa para guardar los mensjaes de respuesta.
		Map<String, Object> response=new HashMap<>();
		Cliente cliente=null;
		
		try {
			cliente=clienteService.findById(id);
		}catch (DataAccessException e) {
		//Error en la base de datos (conexiones, sintaxis,etc).
			response.put("mensaje", "Error al realizar la consulta a la BD [backend]");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String,Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
							
		//Error cliente=null.
		if(cliente==null) {
			response.put("mensaje", "El cliente ID:".concat(id.toString().concat(" No existe en base de datos! [backend]")));
			return new ResponseEntity<Map<String,Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<Cliente>(cliente,HttpStatus.OK);
	}
	
/*	@PostMapping("/clientes")
	@ResponseStatus(HttpStatus.CREATED)
	public Cliente create(@RequestBody Cliente cliente) {
		//cliente.setCreateAt(new Date());//Se agrego el prepersist en la clase Cliente
		return clienteService.save(cliente);
	}*/
	
/*
 * @Valid, Justo antes de invocar el metodo create. // El interceptor de
 * spring, intercepta el objeto cliente y valida cada valor desde el requestbody
 * que esta enviando en formato json. // Sin la anotacion a persar de tener
 * configurado la Entity no se va validar. // @BindingResult, objeto que tiene
 * es resultado de la validacion
 */	
	
	@PostMapping("/clientes")
	public ResponseEntity<?> create(@Valid @RequestBody Cliente cliente, BindingResult result) {		
		Cliente clienteNew=null;
		Map<String, Object> response=new HashMap<>();		
		System.out.println("Backend: Cliente.createAt="+cliente.getCreateAt());
		if (result.hasErrors()) {
			
			/*
			 * List<String> errors=new ArrayList<>();
			 * 
			 * for (FieldError err : result.getFieldErrors()) {
			 * errors.add("El campo '"+err.getField()+"' "+err.getDefaultMessage()); }
			 */
			 			
			List<String> errors=result.getFieldErrors()
					.stream()
					.map(err->{ //Por cada elemento del flujo 'fieldError' convertir a String
						return "El campo '"+err.getField()+"' "+err.getDefaultMessage();
						})
					.collect(Collectors.toList());
			
			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST); //Error 400
		}
						
		try {
			 clienteNew=clienteService.save(cliente);
		} catch (DataAccessException e) {
			response.put("mensaje", "Error al realizar el insert en la base de datos [backend]");
			response.put("error", e.getMessage().concat(":. ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje","El cliente ha sido creado con exito! [backend]");
		response.put("cliente", clienteNew);
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	
	/*
	 * @PutMapping("/clientes/{id}")
	 * 
	 * @ResponseStatus(HttpStatus.CREATED) public Cliente update(@RequestBody
	 * Cliente cliente, @PathVariable Long id) { Cliente
	 * clienteActual=clienteService.findById(id);
	 * clienteActual.setApellido(cliente.getApellido());
	 * clienteActual.setNombre(cliente.getNombre());
	 * clienteActual.setEmail(cliente.getEmail());
	 * 
	 * return clienteService.save(clienteActual); }
	 */
	
	//Importante el orden @Valid y BindingResult
	@PutMapping("/clientes/{id}")
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<?> update(@Valid @RequestBody Cliente cliente, BindingResult result , @PathVariable Long id) {
		Cliente clienteActual=clienteService.findById(id);
		Cliente clienteUpdated=null;
		
		Map<String, Object> response=new HashMap<>();
		
		if (result.hasErrors()) {
			List<String> errors=result.getFieldErrors()
					.stream()
					.map(err->"El campo '"+err.getField()+"' "+err.getDefaultMessage())
					.collect(Collectors.toList());
			
			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}
		
		if (clienteActual==null) {
			response.put("mensaje", "Error: no se puede editar, el cliente ID:".concat(id.toString().concat("no existe en la base de datos [backend]")));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		
		try {
			clienteActual.setApellido(cliente.getApellido());
			clienteActual.setNombre(cliente.getNombre());
			clienteActual.setEmail(cliente.getEmail());
			clienteActual.setCreateAt(cliente.getCreateAt());
			
			clienteUpdated=clienteService.save(clienteActual);
			
		} catch (DataAccessException e) {
			response.put("mensaje", "Error al actualizar el cliente en la base de datos [backend]");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "El cliente ha sido actualizado con exito! [backend]");
		response.put("cliente", clienteUpdated);
				
		return new ResponseEntity<Map<String, Object>>(response,HttpStatus.CREATED); 
	}
	
	
	/*
	 * @DeleteMapping("/clientes/{id}")
	 * 
	 * @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long
	 * id) { clienteService.delete(id); }
	 */
	
	@DeleteMapping("/clientes/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<?> delete(@PathVariable Long id) {
		Map<String,Object> response=new HashMap<>();
		
		try {
			clienteService.delete(id);	
		} catch (DataAccessException e) {
			response.put("mensaje", "Error al eliminar el cliente de la base de datos [backend]");
			response.put("error",e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "El cliente eliminado con exito! [backend]");
		//return new ResponseEntity<Map<String,Object>>(response,HttpStatus.OK);
		return new ResponseEntity<>(response,HttpStatus.OK);
		
	}
	
	@PostMapping("/clientes/upload")
	public ResponseEntity<?> upload(@RequestParam("archivo") MultipartFile archivo, @RequestParam("id") long id){
		//Map<String, Object> response=new HashMap<>();
		Map<String, Object> response=new HashMap<String,Object>();	
		
		Cliente cliente=clienteService.findById(id);
		
		if(!archivo.isEmpty()) {
			String nombreArchivo=UUID.randomUUID().toString()+"_"+ archivo.getOriginalFilename().replace(" ","");
			Path rutaArchivo=Paths.get("uploads").resolve(nombreArchivo).toAbsolutePath();
			try {
				Files.copy(archivo.getInputStream(), rutaArchivo);	
			} catch (Exception e) {
				response.put("mensaje", "Error al subir la imagen del cliente"+nombreArchivo);
				response.put("error", e.getMessage().concat(": ").concat(e.getCause().getMessage()));
				return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}					
			cliente.setFoto(nombreArchivo);
			
			clienteService.save(cliente);
			
			response.put("cliente", cliente);
			response.put("mensaje","Has subido correctamente la imagen: "+nombreArchivo);
		}		
		return new ResponseEntity<>(response,HttpStatus.CREATED);
		//return new ResponseEntity<Map<String,Object>>(response,HttpStatus.CREATED);				
	}
}
