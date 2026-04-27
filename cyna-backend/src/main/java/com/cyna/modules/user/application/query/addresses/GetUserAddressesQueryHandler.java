package com.cyna.modules.user.application.query.addresses;

import com.cyna.modules.user.domain.repository.AddressRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetUserAddressesQueryHandler implements QueryHandler<GetUserAddressesQuery, List<AddressReadModel>> {

    private final AddressRepository addressRepository;

    public GetUserAddressesQueryHandler(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public List<AddressReadModel> handle(GetUserAddressesQuery query) {
        return addressRepository.findAllByUserId(query.userId()).stream()
                .map(a -> new AddressReadModel(
                        a.getId(), a.getLabel(), a.getAddress(), a.getAddress2(),
                        a.getZipCode(), a.getCity(), a.getRegion(), a.getCountryCode(),
                        a.getPhone(), a.isDefault(), a.getCreatedAt(), a.getUpdatedAt()
                ))
                .toList();
    }
}
