package org.lazydoc.example.spring.controller;

import org.lazydoc.example.spring.entity.Customer;
import org.lazydoc.example.spring.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class RestfulCustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @RequestMapping(value = "/customers", method = RequestMethod.GET)
    public @ResponseBody List<Customer> getCustomers() {
        return customerRepository.findAll();
    }

    @RequestMapping(value = "/customers", method = RequestMethod.POST)
    public @ResponseBody Customer createCustomer(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }

    @RequestMapping(value = "/customers/{customerId}", method = RequestMethod.GET)
    public @ResponseBody Customer getCustomer(@PathVariable long customerId) {
        return customerRepository.findOne(customerId);
    }

    @RequestMapping(value = "/customers/{customerId}", method = RequestMethod.PUT)
    public @ResponseBody Customer updateCustomer(@PathVariable long customerId, @RequestBody Customer customer) {
        customer.setCustomerId(customerId);
        return customerRepository.save(customer);
    }

    @RequestMapping(value = "/customers/{customerId}", method = RequestMethod.DELETE)
    public @ResponseBody void deleteCustomer(@PathVariable long customerId) {
        customerRepository.delete(customerId);
    }





}
