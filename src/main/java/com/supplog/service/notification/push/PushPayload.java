package com.supplog.service.notification.push;

import java.util.Map;

/**
 * Bir cihaza gidecek bildirim. {@code data} yalnızca kimlik/tür gibi hassas olmayan alanları taşır;
 * metin {@code title}/{@code body} içindedir. {@code tag} aynı olayın önceki bildirimini tepside değiştirir.
 */
public record PushPayload(
        String title,
        String body,
        Map<String, String> data,
        String tag,
        String channelId
) {
}
