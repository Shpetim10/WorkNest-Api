package com.worknest.features.subscription.application;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.PaymentMethod;
import com.stripe.model.billingportal.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.worknest.features.subscription.exception.StripeException;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeServiceImpl implements StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    void init() {
        Stripe.apiKey = secretKey;
    }

    @Override
    public Customer createCustomer(String email, String companyName, String companyId) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(companyName)
                    .putMetadata("company_id", companyId)
                    .build();
            return Customer.create(params);
        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to create Stripe customer for company {}: {}", companyId, e.getMessage());
            throw new StripeException("Failed to create billing customer: " + e.getMessage());
        }
    }

    @Override
    public com.stripe.model.Subscription createSubscriptionWithTrial(String customerId, String priceId, String paymentMethodId) {
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            pm.attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build());

            Customer customer = Customer.retrieve(customerId);
            CustomerUpdateParams updateParams = CustomerUpdateParams.builder()
                    .setInvoiceSettings(
                            CustomerUpdateParams.InvoiceSettings.builder()
                                    .setDefaultPaymentMethod(paymentMethodId)
                                    .build()
                    )
                    .build();
            customer.update(updateParams);

            long trialEndEpoch = Instant.now().plusSeconds(30L * 24 * 60 * 60).getEpochSecond();

            SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                    .setTrialEnd(trialEndEpoch)
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                            .setSaveDefaultPaymentMethod(SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                            .build())
                    .addExpand("latest_invoice.payment_intent")
                    .build();

            return com.stripe.model.Subscription.create(params);
        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to create Stripe subscription for customer {}: {}", customerId, e.getMessage());
            throw new StripeException("Failed to create subscription: " + e.getMessage());
        }
    }

    @Override
    public void cancelSubscription(String stripeSubscriptionId) {
        try {
            com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            subscription.cancel();
        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to cancel Stripe subscription {}: {}", stripeSubscriptionId, e.getMessage());
            throw new StripeException("Failed to cancel subscription: " + e.getMessage());
        }
    }

    @Override
    public Session createBillingPortalSession(String customerId, String returnUrl) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setReturnUrl(returnUrl)
                    .build();
            return Session.create(params);
        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to create billing portal session for customer {}: {}", customerId, e.getMessage());
            throw new StripeException("Failed to create billing portal session: " + e.getMessage());
        }
    }

    @Override
    public Event constructWebhookEvent(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new com.worknest.features.subscription.exception.StripeException("Invalid Stripe webhook signature");
        }
    }
}
