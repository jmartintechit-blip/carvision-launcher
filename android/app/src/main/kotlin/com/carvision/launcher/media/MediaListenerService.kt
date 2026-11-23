package com.carvision.launcher.media

import android.service.notification.NotificationListenerService

/**
 * Servicio vacío: Android solo deja leer las sesiones multimedia de otras apps (lo que suena, carátula,
 * controles) a las apps con "acceso a notificaciones". CarVision no lee ni guarda las notificaciones.
 */
class MediaListenerService : NotificationListenerService()
