package com.worknest.features.subscription.application;

import com.stripe.model.Customer;
import com.stripe.model.Event;

public interface StripeService {

    Customer createCustomer(String email, String companyName, String companyId);

    com.stripe.model.Subscription createSubscriptionWithTrial(String customerId, String priceId, String paymentMethodId);

    void cancelSubscription(String stripeSubscriptionId);

    com.stripe.model.billingportal.Session createBillingPortalSession(String customerId, String returnUrl);

    Event constructWebhookEvent(String payload, String sigHeader);
}
