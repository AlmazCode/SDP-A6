package social;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public final class Main {
    public static void main(String[] args) throws Exception {
        int threads = args.length == 0 ? 3 : Integer.parseInt(args[0]);
        if (threads < 3) {
            throw new IllegalArgumentException("Use at least 3 producer threads");
        }

        Group java = new Group("java", List.of(
                new TextPost("p1", "Observer and Visitor", 10),
                new Comment("c1", "SPAM offer", 2),
                new Image("i1", "Class diagram", 5),
                new Video("v1", "Pattern tutorial", 7, 30)));
        Group music = new Group("music", List.of(new TextPost("p2", "New song", 3)));
        Community community = new Community("students", List.of(java, music));

        PostSource posts = new PostSource();
        ReactionSource reactions = new ReactionSource();
        EventSource reports = new EventSource();
        Feed feed = new Feed();
        NotificationCenter notifications = new NotificationCenter();
        EventLog log = new EventLog();
        TrendingTracker trending = new TrendingTracker(community.children());
        ModerationService moderation = new ModerationService(reports);

        posts.subscribe(EventType.POST_CREATED, feed);
        posts.subscribe(EventType.POST_CREATED, notifications);
        posts.subscribe(EventType.POST_CREATED, moderation);
        posts.subscribe(EventType.POST_CREATED, log);
        reactions.subscribe(EventType.REACTION_ADDED, notifications);
        reactions.subscribe(EventType.REACTION_ADDED, trending);
        reactions.subscribe(EventType.REACTION_ADDED, log);
        trending.subscribe(EventType.TREND_UPDATED, notifications);
        trending.subscribe(EventType.TREND_UPDATED, log);
        reports.subscribe(EventType.MODERATION_COMPLETED, notifications);
        reports.subscribe(EventType.MODERATION_COMPLETED, log);

        List<Runnable> jobs = new ArrayList<>();
        for (int worker = 0; worker < threads; worker++) {
            int producerId = worker;
            jobs.add(() -> {
                for (int i = 0; i < 100; i++) {
                    if (producerId % 2 == 0) {
                        posts.post(java, "p1");
                    } else {
                        reactions.react(java, "p1");
                    }
                }
            });
        }
        new Producers(Executors.newFixedThreadPool(threads)).run(jobs);

        System.out.println("Producer threads: " + threads);
        System.out.println("Feed events: " + feed.posts().size());
        System.out.println("Reactions in java: " + trending.reactions("java"));
        System.out.println("Notifications: " + notifications.count());
        System.out.println("Logged events: " + log.events().size());
        System.out.println("Engagement: " + community.accept(new EngagementVisitor()));
        System.out.println("Content count: " + community.accept(new ContentCountVisitor()));
        ModerationVisitor review = new ModerationVisitor();
        community.accept(review);
        System.out.println("Moderation: reviewed=" + review.reviewed()
                + ", flagged=" + review.flaggedIds());
        System.out.println("Popular content: " + community.accept(new PopularContentVisitor(5))
                .stream().map(Element::id).toList());
        System.out.println("JSON: " + community.accept(new JsonVisitor()));
        System.out.println("All producers finished.");
    }
}
