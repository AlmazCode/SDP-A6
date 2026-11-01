package social;

public final class ModerationService implements Observer<SocialEvent> {
    private final Subject<SocialEvent> reports;

    public ModerationService(Subject<SocialEvent> reports) {
        this.reports = reports;
    }

    @Override
    public void onEvent(SocialEvent event) {
        ModerationVisitor visitor = new ModerationVisitor();
        event.group().accept(visitor);
        reports.publish(new SocialEvent(EventType.MODERATION_COMPLETED, event.group(),
                "reviewed=" + visitor.reviewed() + ", flagged=" + visitor.flaggedIds(),
                visitor.flaggedIds().size()));
    }
}
