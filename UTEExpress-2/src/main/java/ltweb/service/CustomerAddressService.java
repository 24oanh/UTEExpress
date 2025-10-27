package ltweb.service;

import ltweb.dto.CustomerAddressDTO;
import ltweb.entity.Customer;
import ltweb.entity.CustomerAddress;
import ltweb.repository.CustomerAddressRepository;
import ltweb.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {
    
    private final CustomerAddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    
    public List<CustomerAddress> getAddressesByCustomerId(Long customerId) {
        return addressRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId);
    }
    
    public CustomerAddress getAddressById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại"));
    }
    
    @Transactional
    public CustomerAddress createAddress(Long customerId, CustomerAddressDTO dto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
        
        // Nếu đây là địa chỉ đầu tiên hoặc được đặt làm mặc định
        if (dto.getIsDefault() || addressRepository.countByCustomerId(customerId) == 0) {
            // Bỏ mặc định của các địa chỉ khác
            addressRepository.findByCustomerIdAndIsDefault(customerId, true)
                    .ifPresent(addr -> {
                        addr.setIsDefault(false);
                        addressRepository.save(addr);
                    });
            dto.setIsDefault(true);
        }
        
        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .label(dto.getLabel())
                .recipientName(dto.getRecipientName())
                .recipientPhone(dto.getRecipientPhone())
                .address(dto.getAddress())
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .build();
        
        return addressRepository.save(address);
    }
    
    @Transactional
    public CustomerAddress updateAddress(Long customerId, Long addressId, CustomerAddressDTO dto) {
        CustomerAddress address = getAddressById(addressId);
        
        if (!address.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Không có quyền chỉnh sửa địa chỉ này");
        }
        
        // Nếu đặt làm mặc định
        if (dto.getIsDefault() && !address.getIsDefault()) {
            addressRepository.findByCustomerIdAndIsDefault(customerId, true)
                    .ifPresent(addr -> {
                        addr.setIsDefault(false);
                        addressRepository.save(addr);
                    });
        }
        
        address.setLabel(dto.getLabel());
        address.setRecipientName(dto.getRecipientName());
        address.setRecipientPhone(dto.getRecipientPhone());
        address.setAddress(dto.getAddress());
        address.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : address.getIsDefault());
        
        return addressRepository.save(address);
    }
    
    @Transactional
    public void deleteAddress(Long customerId, Long addressId) {
        CustomerAddress address = getAddressById(addressId);
        
        if (!address.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Không có quyền xóa địa chỉ này");
        }
        
        boolean wasDefault = address.getIsDefault();
        addressRepository.delete(address);
        
        // Nếu xóa địa chỉ mặc định, đặt địa chỉ khác làm mặc định
        if (wasDefault) {
            List<CustomerAddress> remaining = addressRepository
                    .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId);
            if (!remaining.isEmpty()) {
                CustomerAddress newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }
    
    @Transactional
    public CustomerAddress setDefaultAddress(Long customerId, Long addressId) {
        CustomerAddress address = getAddressById(addressId);
        
        if (!address.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Không có quyền thao tác địa chỉ này");
        }
        
        // Bỏ mặc định của địa chỉ khác
        addressRepository.findByCustomerIdAndIsDefault(customerId, true)
                .ifPresent(addr -> {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                });
        
        address.setIsDefault(true);
        return addressRepository.save(address);
    }
}