package org.lazydoc.example.spring.controller;

import org.lazydoc.annotation.*;
import org.lazydoc.example.spring.entity.Customer;
import org.lazydoc.example.spring.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@DomainDescription(name="Customer", shortDescription = "Customer management", description = "You can manage all your customers with these operations")
public class RestfulCustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @RequestMapping(value = "/customers", method = RequestMethod.GET)
    @OperationDescription(description = "This operation returns all customers")
    @ResponseDescription(description = "This operation returns status code 200 and a list of customers")
    public @ResponseBody List<Customer> getCustomers() {
        return customerRepository.findAll();
    }

    @RequestMapping(value = "/customers", method = RequestMethod.POST)
    @OperationDescription(description = "This operation creates a new customer")
    @ParameterDescription({@Parameter(name = "requestBody", description = "The customer to be created")})
    @ResponseDescription(description = "This operation returns status code 200 and the created customer")
    public @ResponseBody Customer createCustomer(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }

    @RequestMapping(value = "/customers/{customerId}", method = RequestMethod.GET)
    @OperationDescription(description = "This operation returns the requested customer")
    @ParameterDescription({@Parameter(name = "customerId", description = "The id of the customer to be returned")})
    @ResponseDescription(description = "This operation returns status code 200 and the requested customer")
    public @ResponseBody Customer getCustomer(@PathVariable long customerId) {
        return customerRepository.findOne(customerId);
    }

    @RequestMapping(value = "/customers/{customerId}", method = RequestMethod.POST)
    @OperationDescription(description = "This operation updates a customer")
    @ParameterDescription({@Parameter(name = "customerId", description = "The id of the customer to be updated"),
    @Parameter(name = "requestBody", description = "The data of the customer which should be updated")})
    @ResponseDescription(description = "This operation returns status code 200 and the updated customer")
    public @ResponseBody Customer updateCustomer(@PathVariable long customerId, @RequestBody Customer customer) {
        customer.setCustomerId(customerId);
        return customerRepository.save(customer);
    }

    @RequestMapping(value = "/customers/{customerId}", method = RequestMethod.DELETE)
    @OperationDescription(description = "This operation deletes the requested customer")
    @ParameterDescription({@Parameter(name = "customerId", description = "The id of the customer to be returned")})
    @ResponseDescription(description = "This operation returns status code 200")
    public @ResponseBody void deleteCustomer(@PathVariable long customerId) {
        customerRepository.delete(customerId);
    }




}
