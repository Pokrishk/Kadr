package com.example.Kadr.controller.api;

import com.example.Kadr.model.Address;
import com.example.Kadr.repository.AddressRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Адреса", description = "Операции с адресами площадок")
public class AddressApiController extends AbstractCrudApiController<Address> {

    private final AddressRepository addressRepository;

    public AddressApiController(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    protected JpaRepository<Address, Long> getRepository() {
        return addressRepository;
    }

    @Override
    protected String getResourceName() {
        return "Адрес";
    }
}