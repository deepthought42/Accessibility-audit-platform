package com.looksee.journeyErrors.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;


@Service
@PropertySource("classpath:application.properties")
public class StripeService {
	@Value("${stripe.checkout_success_url}")
    private String checkout_success_url;
	
	@Value("${stripe.checkout_cancel_url}")
    private String checkout_cancel_url;
	/*
    @Autowired
    StripeService(@Value("${stripe.secretKey}") String secretKey) {
    	//TEST
    	Stripe.apiKey = secretKey;
    }
    
    public void update_subscription(String price_id, Subscription subscription) 
    		throws StripeException{
    	SubscriptionUpdateParams params =
	    	SubscriptionUpdateParams.builder()
		        .setCancelAtPeriodEnd(false)
		        .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
		        .addItem(
		          SubscriptionUpdateParams.Item.builder()
		            .setId(subscription.getItems().getData().get(0).getId())
		            //.setPrice(price_id)
		            .build())
		        .build();
    	subscription.update(params);
    }
    
    public Subscription subscribe(String price_id, Customer customer) 
    		throws StripeException{
    	List<Object> items = new ArrayList<>();
		Map<String, Object> item1 = new HashMap<>();
		item1.put(
		  "price",
		  price_id
		);
		items.add(item1);
		Map<String, Object> params = new HashMap<>();
		params.put("customer", customer.getId());
		params.put("items", items);

		return Subscription.create(params);
    }

    public Subscription subscribe(Plan discovery, Plan tests, Customer customer) 
    		throws StripeException{
    	Map<String, Object> discovery_plan = new HashMap<String, Object>();
    	discovery_plan.put("plan", discovery.getId());

    	Map<String, Object> test_plan = new HashMap<String, Object>();
    	test_plan.put("plan", tests.getId());
    	
    	Map<String, Object> items = new HashMap<String, Object>();
    	items.put("0", discovery_plan);
    	items.put("1", test_plan);
    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("customer", customer.getId());
    	params.put("items", items);

		return Subscription.create(params);
    }
    
    public Customer createCustomer(String token, String email) throws Exception {
        Map<String, Object> customerParams = new HashMap<String, Object>();
        customerParams.put("email", email);
        if(token != null){
        	customerParams.put("source", token);
        }
        
    	return Customer.create(customerParams);
    }

    public Customer getCustomer(String customer_id) throws Exception {
        return Customer.retrieve(customer_id);
    }

	public Subscription getSubscription(String subscriptionToken) 
			throws StripeException {
		return Subscription.retrieve(subscriptionToken);
	}
	
	public Subscription cancelSubscription(String subscription_token) throws StripeException {
		Subscription subscription = Subscription.retrieve(subscription_token);
		return subscription.cancel();
	}

	public Customer deleteCustomer(String customer_token) throws StripeException {
		Customer customer = Customer.retrieve(customer_token);
		return customer.delete();
	}
*/
	/**
	 * Creates a session for Stripe Checkout and returns the session id
	 * @param price_id
	 * @param customer_id TODO
	 * @param customer_email TODO
	 * @return
	 * @throws StripeException
	 */
	/*
	public Session createCheckoutSession(String price_id, 
										 String customer_id, 
										 String customer_email
	 ) throws StripeException {
		if(customer_id != null && !customer_id.isEmpty()) {
			SessionCreateParams params = new SessionCreateParams.Builder()
					  .setSuccessUrl(checkout_success_url)
					  .setCancelUrl(checkout_cancel_url)
					  .setCustomer(customer_id)
					  .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
					  .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
					  .setAllowPromotionCodes(Boolean.TRUE)
					  .addLineItem(new SessionCreateParams.LineItem.Builder()
					    // For metered billing, do not pass quantity
					    .setQuantity(1L)
					    .setPrice(price_id)
					    .build()
					  )
					  .build();
			return Session.create(params);
		}
		else {
			SessionCreateParams params = new SessionCreateParams.Builder()
					  .setSuccessUrl(checkout_success_url)
					  .setCancelUrl(checkout_cancel_url)
					  .setCustomerEmail(customer_email)
					  .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
					  .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
					  .addLineItem(new SessionCreateParams.LineItem.Builder()
					    // For metered billing, do not pass quantity
					    .setQuantity(1L)
					    .setPrice(price_id)
					    .build()
					  )
					  .build();
			return Session.create(params);
		}
	}

	public Price getPrice(String price_id) throws StripeException {
		return Price.retrieve(price_id);
	}

	public Product getProduct(String product_id) throws StripeException {
		return Product.retrieve(product_id);
	}
	*/
}
