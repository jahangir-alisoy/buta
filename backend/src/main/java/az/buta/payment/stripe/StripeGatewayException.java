package az.buta.payment.stripe;

public class StripeGatewayException extends RuntimeException {

    public StripeGatewayException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
