class NotificationSound {
    constructor() {
        this.audio = new Audio('/sounds/notification.mp3');
    }

    play() {
        this.audio.play().catch(e => console.log('Could not play sound:', e));
    }
}

const notificationSound = new NotificationSound();