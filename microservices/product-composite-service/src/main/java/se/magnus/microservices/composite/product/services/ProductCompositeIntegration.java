package se.magnus.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;

import java.io.IOException;

import static java.util.logging.Level.FINE;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final ObjectMapper mapper;
    private final WebClient webClient;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    @Autowired
    public ProductCompositeIntegration(ObjectMapper mapper, WebClient.Builder webClient, @Value("${app.product-service.host}") String productServiceHost, @Value("${app.product-service.port}") int productServicePort, @Value("${app.recommendation-service.host}") String recommendationServiceHost, @Value("${app.recommendation-service.port}") int recommendationServicePort, @Value("${app.review-service.host}") String reviewServiceHost, @Value("${app.review-service.port}") int reviewServicePort) {
        this.webClient = webClient.build();
        this.mapper = mapper;

        productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product";
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation";
        reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review";
    }

    @Override
    public Mono<Product> createProduct(Product body) {
        String url = productServiceUrl;
        LOG.debug("Will post a new product to URL: {}", url);

        return webClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Product.class)
                .log(LOG.getName(), FINE)
                .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));

    }


    public Mono<Product> getProduct(int productId) {
        String url = productServiceUrl + "/product/" + productId;
        LOG.debug("Will call the getProduct API on URL: {}", url);

        return webClient.get().uri(url).retrieve().bodyToMono(Product.class).log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }


    @Override
    public Mono<Void> deleteProduct(int productId) {
        String url = productServiceUrl + "/" + productId;
        LOG.debug("Will call the deleteProduct API on URL: {}", url);

        return webClient.delete().uri(url).retrieve().bodyToMono(Void.class).log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, ex -> handleException(ex));

    }

    private Throwable handleException(Throwable ex) {
        if (!(ex instanceof WebClientResponseException)) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }

        WebClientResponseException wcre = (WebClientResponseException) ex;

        switch (HttpStatus.resolve(wcre.getStatusCode().value())) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(wcre));

            default:
                LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {

        String url = recommendationServiceUrl;
        LOG.debug("Will post a new recommendation to URL: {}", url);

        return webClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Recommendation.class)
                .log(LOG.getName(), FINE)
                .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));

    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {

        String url = recommendationServiceUrl + "?productId=" + productId;

        LOG.debug("Will call the getRecommendations API on URL: {}", url);

        return webClient.get().uri(url).retrieve().bodyToFlux(Recommendation.class).log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, ex -> handleException(ex));

    }

    @Override
    public Mono<Void> deleteRecommendations(int productId) {
        String url = recommendationServiceUrl + "?productId=" + productId;
        LOG.debug("Will call the deleteRecommendations API on URL: {}", url);

        return webClient.delete().uri(url).retrieve().bodyToMono(Void.class).log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, ex -> handleException(ex));

    }

    @Override
    public Mono<Review> createReview(Review body) {

        String url = reviewServiceUrl;
        LOG.debug("Will post a new review to URL: {}", url);

        return webClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Review.class)
                .log(LOG.getName(), FINE)
                .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }

    @Override
    public Flux<Review> getReviews(int productId) {

        String url = reviewServiceUrl + "?productId=" + productId;

        LOG.debug("Will call the getReviews API on URL: {}", url);

        return webClient.get().uri(url).retrieve().bodyToFlux(Review.class).log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }

    @Override
    public Mono<Void> deleteReviews(int productId) {
        String url = reviewServiceUrl + "?productId=" + productId;
        LOG.debug("Will call the deleteReviews API on URL: {}", url);
        return webClient.delete().uri(url).retrieve().bodyToMono(Void.class).log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }
}
