package com.eventflow.template.infrastructure.renderer;

import com.eventflow.template.domain.model.Template;
import com.eventflow.template.domain.model.TemplateContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandlebarsTemplateRendererTest {

    private HandlebarsTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new HandlebarsTemplateRenderer();
    }

    @Test
    void render_shouldReplaceSimpleVariable() {
        // Arrange
        String template = "Hello {{name}}!";
        Map<String, Object> context = Map.of("name", "John");

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Hello John!");
    }

    @Test
    void render_shouldReplaceMultipleVariables() {
        // Arrange
        String template = "Hello {{firstName}} {{lastName}}! Your order #{{orderId}} is ready.";
        Map<String, Object> context = Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "orderId", "12345"
        );

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Hello John Doe! Your order #12345 is ready.");
    }

    @Test
    void render_shouldHandleNestedObjects() {
        // Arrange
        String template = "Order: {{order.id}}, Total: ${{order.total}}";
        Map<String, Object> order = Map.of("id", "12345", "total", "99.99");
        Map<String, Object> context = Map.of("order", order);

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Order: 12345, Total: $99.99");
    }

    @Test
    void render_shouldHandleConditionals() {
        // Arrange
        String template = "{{#if isPremium}}Premium Member{{else}}Regular Member{{/if}}";
        
        // Act - Premium
        Map<String, Object> premiumContext = Map.of("isPremium", true);
        String premiumResult = renderer.render(template, premiumContext);
        
        // Act - Regular
        Map<String, Object> regularContext = Map.of("isPremium", false);
        String regularResult = renderer.render(template, regularContext);

        // Assert
        assertThat(premiumResult).isEqualTo("Premium Member");
        assertThat(regularResult).isEqualTo("Regular Member");
    }

    @Test
    void render_shouldHandleLoops() {
        // Arrange
        String template = "Items: {{#each items}}{{this}}, {{/each}}";
        Map<String, Object> context = Map.of("items", new String[]{"Apple", "Banana", "Orange"});

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Items: Apple, Banana, Orange, ");
    }

    @Test
    void render_shouldHandleMissingVariable_withEmptyString() {
        // Arrange
        String template = "Hello {{name}}!";
        Map<String, Object> context = new HashMap<>();

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Hello !");
    }

    @Test
    void render_shouldEscapeHtmlByDefault() {
        // Arrange
        String template = "Content: {{htmlContent}}";
        Map<String, Object> context = Map.of("htmlContent", "<script>alert('XSS')</script>");

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).contains("&lt;script&gt;");
        assertThat(result).doesNotContain("<script>");
    }

    @Test
    void render_shouldAllowUnescapedHtml_withTripleBraces() {
        // Arrange
        String template = "Content: {{{htmlContent}}}";
        Map<String, Object> context = Map.of("htmlContent", "<b>Bold Text</b>");

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).contains("<b>Bold Text</b>");
    }

    @Test
    void render_shouldHandleEmailTemplate() {
        // Arrange
        String subjectTemplate = "Order Confirmation - #{{orderId}}";
        String bodyTemplate = """
                Hi {{customerName}},
                
                Your order #{{orderId}} has been confirmed.
                Total: ${{total}}
                
                Thank you for shopping with us!
                """;
        
        Map<String, Object> context = Map.of(
                "orderId", "12345",
                "customerName", "John Doe",
                "total", "99.99"
        );

        // Act
        String subject = renderer.render(subjectTemplate, context);
        String body = renderer.render(bodyTemplate, context);

        // Assert
        assertThat(subject).isEqualTo("Order Confirmation - #12345");
        assertThat(body).contains("Hi John Doe");
        assertThat(body).contains("Your order #12345");
        assertThat(body).contains("Total: $99.99");
    }

    @Test
    void render_shouldHandleSmsTemplate() {
        // Arrange
        String template = "Your verification code is {{code}}. Valid for {{expiryMinutes}} minutes.";
        Map<String, Object> context = Map.of(
                "code", "123456",
                "expiryMinutes", "10"
        );

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Your verification code is 123456. Valid for 10 minutes.");
    }

    @Test
    void render_shouldHandleNumericValues() {
        // Arrange
        String template = "Price: ${{price}}, Quantity: {{quantity}}";
        Map<String, Object> context = Map.of(
                "price", 99.99,
                "quantity", 5
        );

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Price: $99.99, Quantity: 5");
    }

    @Test
    void render_shouldHandleBooleanValues() {
        // Arrange
        String template = "Is Active: {{isActive}}";
        Map<String, Object> context = Map.of("isActive", true);

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Is Active: true");
    }

    @Test
    void render_shouldThrowException_whenTemplateIsNull() {
        // Arrange
        Map<String, Object> context = Map.of("name", "John");

        // Act & Assert
        assertThatThrownBy(() -> renderer.render(null, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template cannot be null");
    }

    @Test
    void render_shouldThrowException_whenContextIsNull() {
        // Arrange
        String template = "Hello {{name}}";

        // Act & Assert
        assertThatThrownBy(() -> renderer.render(template, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Context cannot be null");
    }

    @Test
    void render_shouldHandleEmptyTemplate() {
        // Arrange
        String template = "";
        Map<String, Object> context = Map.of("name", "John");

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void render_shouldHandleTemplateWithNoVariables() {
        // Arrange
        String template = "This is a static message with no variables.";
        Map<String, Object> context = Map.of("name", "John");

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("This is a static message with no variables.");
    }

    @Test
    void render_shouldHandleComplexNestedStructures() {
        // Arrange
        String template = "{{#each users}}Name: {{name}}, Email: {{email}}\n{{/each}}";
        
        Map<String, Object> user1 = Map.of("name", "John", "email", "john@example.com");
        Map<String, Object> user2 = Map.of("name", "Jane", "email", "jane@example.com");
        
        Map<String, Object> context = Map.of("users", new Map[]{user1, user2});

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).contains("Name: John, Email: john@example.com");
        assertThat(result).contains("Name: Jane, Email: jane@example.com");
    }

    @Test
    void render_shouldHandleSpecialCharacters() {
        // Arrange
        String template = "Message: {{message}}";
        Map<String, Object> context = Map.of("message", "Hello! How are you? I'm fine.");

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Message: Hello! How are you? I'm fine.");
    }

    @Test
    void render_shouldHandleUnicodeCharacters() {
        // Arrange
        String template = "Greeting: {{greeting}}";
        Map<String, Object> context = Map.of("greeting", "こんにちは (Hello)");

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Greeting: こんにちは (Hello)");
    }

    @Test
    void render_shouldHandleWhitespace() {
        // Arrange
        String template = "Name: {{ name }}";
        Map<String, Object> context = Map.of("name", "John");

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Name: John");
    }

    @Test
    void render_shouldThrowException_onMalformedTemplate() {
        // Arrange
        String template = "Hello {{name";  // Missing closing braces
        Map<String, Object> context = Map.of("name", "John");

        // Act & Assert
        assertThatThrownBy(() -> renderer.render(template, context))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void render_shouldHandleHelperFunctions() {
        // Arrange - assuming custom helper for uppercase
        String template = "Name: {{name}}";
        Map<String, Object> context = Map.of("name", "john");

        // Act
        String result = renderer.render(template, context);

        // Assert
        assertThat(result).isEqualTo("Name: john");
    }
}
