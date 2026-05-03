package com.cyna.modules.user.application.query.addresses;

import com.cyna.shared.application.Query;

import java.util.List;
import java.util.UUID;

public record GetUserAddressesQuery(UUID userId) implements Query<List<AddressReadModel>> {}
