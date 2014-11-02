package org.lazydoc.example.spring.controller;

import org.lazydoc.annotation.DomainDescription;
import org.lazydoc.annotation.OperationDescription;
import org.lazydoc.annotation.Parameter;
import org.lazydoc.annotation.ParameterDescription;
import org.lazydoc.example.spring.entity.Customer;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by eckart on 30.10.14.
 */
@DomainDescription(name = "Customer", description = "Operations to manage customers")
public abstract class RestfulCustomerControllerDocumentation {
    @OperationDescription(description = "This operation returns all customers")
    abstract List<Customer> getCustomers();

    @OperationDescription(description = "This operation creates a new customer")
    @ParameterDescription({@Parameter(name = "requestBody", description = "The customer to be created")})
    abstract Customer createCustomer(@RequestBody Customer customer);

    @OperationDescription(description = "This operation creates a new customer")
    @ParameterDescription({@Parameter(name = "customerId", description = "The customer to be created")})
    abstract Customer getCustomer(@PathVariable long customerId);

    @OperationDescription(description = "This operation creates a new customer")
    @ParameterDescription({@Parameter(name = "requestBody", description = "The customer to be created"),
            @Parameter(name = "customerId", description = "The customer id")})
    abstract Customer updateCustomer(@PathVariable long customerId, @RequestBody Customer customer);

    @OperationDescription(description = "This operation creates a new customer")
    @ParameterDescription({@Parameter(name = "customerId", description = "The customer to be created")})
    abstract void deleteCustomer(@PathVariable long customerId);
}
