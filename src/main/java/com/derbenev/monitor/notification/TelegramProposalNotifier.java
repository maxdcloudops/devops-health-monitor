package com.derbenev.monitor.notification;

import com.derbenev.monitor.model.TradeProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/**
 * Шлёт сообщение в Telegram-чат владельца, когда появляется новое {@link TradeProposal}
 * (от источника сигналов или через API) - чтобы не нужно было самому заходить на дашборд
 * и проверять карточку "Ожидают подтверждения". Approve/reject всё равно делается только
 * там - этот класс только уведомляет, ничего не решает.
 * Отключён по умолчанию (`app.notification.telegram.enabled=false`) - нужен свой токен бота
 * (создаётся через @BotFather) и chat_id.
 */
@Component
public class TelegramProposalNotifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramProposalNotifier.class);
    private static final String API_URL_TEMPLATE = "https://api.telegram.org/bot%s/sendMessage";

    private final HttpClient httpClient;
    private final boolean enabled;
    private final String botToken;
    private final String chatId;

    public TelegramProposalNotifier(
            @Value("${app.notification.telegram.enabled:false}") boolean enabled,
            @Value("${app.notification.telegram.bot-token:}") String botToken,
            @Value("${app.notification.telegram.chat-id:}") String chatId) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.enabled = enabled;
        this.botToken = botToken;
        this.chatId = chatId;
    }

    public void notifyNewProposal(TradeProposal proposal) {
        if (!enabled) {
            return;
        }
        try {
            send(formatMessage(proposal));
        } catch (Exception e) {
            // Сбой уведомления не должен мешать созданию предложения - оно уже сохранено,
            // просто про него никто не узнает мгновенно, увидит на дашборде позже.
            log.warn("Не удалось отправить уведомление в Telegram о предложении {}: {}",
                    proposal.getId(), e.getMessage());
        }
    }

    static String formatMessage(TradeProposal proposal) {
        String botName = proposal.getBot() != null ? proposal.getBot().getName() : "?";
        return String.format(Locale.ROOT,
                "🤖 Новое предложение сделки\nБот: %s\n%s %s x%s по цене %s\n\nОбоснование: %s",
                botName, proposal.getSide(), proposal.getSymbol(), proposal.getQuantity(),
                proposal.getPrice(), proposal.getReasoning());
    }

    private void send(String text) throws IOException, InterruptedException {
        String body = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(API_URL_TEMPLATE, botToken)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Telegram API вернул HTTP " + response.statusCode() + ": " + response.body());
        }
    }
}
