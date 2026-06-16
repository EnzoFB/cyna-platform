package com.cyna.shared.infrastructure.mediator;

import com.cyna.shared.application.Command;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.application.Query;
import com.cyna.shared.application.QueryHandler;
import com.cyna.shared.domain.Result;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Spring-based Mediator implementation.
 * Resolves handlers from the ApplicationContext by naming convention:
 * Command "CreateOrderCommand" → Handler "CreateOrderCommandHandler"
 */
@Component
public class SpringMediator implements Mediator {

    private final ApplicationContext applicationContext;

    public SpringMediator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Result<R> send(Command<R> command) {
        CommandHandler<Command<R>, R> handler = resolveCommandHandler(command);
        return handler.handle(command);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
        QueryHandler<Query<R>, R> handler = resolveQueryHandler(query);
        return handler.handle(query);
    }

    @SuppressWarnings("unchecked")
    private <R> CommandHandler<Command<R>, R> resolveCommandHandler(Command<R> command) {
        String handlerName = command.getClass().getSimpleName() + "Handler";
        return (CommandHandler<Command<R>, R>) applicationContext
                .getBeansOfType(CommandHandler.class)
                .values()
                .stream()
                .filter(h -> h.getClass().getSimpleName().equals(handlerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No handler found for command: " + command.getClass().getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    private <R> QueryHandler<Query<R>, R> resolveQueryHandler(Query<R> query) {
        String handlerName = query.getClass().getSimpleName() + "Handler";
        return (QueryHandler<Query<R>, R>) applicationContext
                .getBeansOfType(QueryHandler.class)
                .values()
                .stream()
                .filter(h -> h.getClass().getSimpleName().equals(handlerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No handler found for query: " + query.getClass().getSimpleName()));
    }
}
