package com.cyna.modules.user.application.api;

import com.cyna.modules.user.domain.repository.AddressRepository;
import com.cyna.modules.user.domain.repository.UserConsentLogRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class UserQueryApiImpl implements UserQueryApi {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserConsentLogRepository userConsentLogRepository;
    private final UserReportingPort reportingPort;

    UserQueryApiImpl(UserRepository userRepository,
                     AddressRepository addressRepository,
                     UserConsentLogRepository userConsentLogRepository,
                     UserReportingPort reportingPort) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.userConsentLogRepository = userConsentLogRepository;
        this.reportingPort = reportingPort;
    }

    @Override
    public long countCustomersCreatedBetween(Instant fromInclusive, Instant toExclusive) {
        return reportingPort.countCustomersCreatedBetween(fromInclusive, toExclusive);
    }

    @Override
    public long countCustomersCreatedBefore(Instant beforeExclusive) {
        return reportingPort.countCustomersCreatedBefore(beforeExclusive);
    }

    @Override
    public List<Integer> findCustomerYears() {
        return reportingPort.findCustomerYears();
    }

    @Override
    public Optional<UserPaymentView> findUserForPayment(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserPaymentView(
                        user.getId(),
                        user.getEmail().value(),
                        user.getFirstName(),
                        user.getLastName()
                ));
    }

    @Override
    public List<UserSummaryView> findSummariesByIds(Collection<UUID> userIds) {
        return userRepository.findAllByIds(userIds).stream()
                .map(u -> new UserSummaryView(
                        u.getId(), u.getEmail().value(), u.getFirstName(), u.getLastName()))
                .toList();
    }

    @Override
    public Optional<UserNotificationView> findUserForNotification(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserNotificationView(
                        user.getId(),
                        user.getEmail().value(),
                        user.getFirstName(),
                        DEFAULT_LANG
                ));
    }

    @Override
    public Optional<UserPersonalDataView> exportPersonalData(UUID userId) {
        return userRepository.findById(userId).map(user -> {
            var account = new UserPersonalDataView.Account(
                    user.getId(), user.getEmail().value(), user.getFirstName(),
                    user.getLastName(), user.getCompany(), user.getRole().name(),
                    user.getStatus().name(), user.getCreatedAt());

            List<UserPersonalDataView.Address> addresses = addressRepository.findAllByUserId(userId)
                    .stream()
                    .map(a -> new UserPersonalDataView.Address(
                            a.getId(), a.getFirstName(), a.getLastName(), a.getLabel(),
                            a.getAddress(), a.getAddress2(), a.getZipCode(), a.getCity(),
                            a.getRegion(), a.getCountryCode(), a.getPhone(), a.getCompany(),
                            a.getVatNumber(), a.isDefault()))
                    .toList();

            List<UserPersonalDataView.Consent> termsConsents = userConsentLogRepository.findAllByUserId(userId)
                    .stream()
                    .map(c -> new UserPersonalDataView.Consent(
                            c.getAction().name(), c.getLabelVersion(),
                            c.getIpAddress(), c.getUserAgent(), c.getGivenAt()))
                    .toList();

            return new UserPersonalDataView(account, addresses, termsConsents);
        });
    }

    private static final String DEFAULT_LANG = "fr";
}
