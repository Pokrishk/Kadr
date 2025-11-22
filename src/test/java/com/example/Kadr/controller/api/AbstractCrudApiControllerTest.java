package com.example.Kadr.controller.api;

import com.example.Kadr.exception.ResourceNotFoundException;
import com.example.Kadr.model.*;
import com.example.Kadr.repository.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AbstractCrudApiControllerTest {

    private record ControllerCase<T>(
            AbstractCrudApiController<T> controller,
            JpaRepository<T, Long> repository,
            T sampleEntity,
            String resourceName) {
    }

    private static <T, R extends JpaRepository<T, Long>> ControllerCase<T> controllerCase(
            R repository,
            Function<R, ? extends AbstractCrudApiController<T>> controllerFactory,
            T sampleEntity,
            String resourceName
    ) {
        AbstractCrudApiController<T> controller = controllerFactory.apply(repository);
        return new ControllerCase<>(controller, repository, sampleEntity, resourceName);
    }

    static Stream<ControllerCase<?>> controllers() {
        return Stream.of(
                controllerCase(mock(ActionRepository.class),       ActionApiController::new,       new Action(),      "Действие"),
                controllerCase(mock(AddressRepository.class),      AddressApiController::new,      new Address(),     "Адрес"),
                controllerCase(mock(CartRepository.class),         CartApiController::new,         new Cart(),        "Корзина"),
                controllerCase(mock(EventRepository.class),        EventApiController::new,        new Event(),       "Событие"),
                controllerCase(mock(EventTypeRepository.class),    EventTypeApiController::new,    new EventType(),   "Тип события"),
                controllerCase(mock(LogEntryRepository.class),     LogEntryApiController::new,     new LogEntry(),    "Запись журнала"),
                controllerCase(mock(OrderEntityRepository.class),  OrderApiController::new,        new OrderEntity(), "Заказ"),
                controllerCase(mock(OrderItemRepository.class),    OrderItemApiController::new,    new OrderItem(),   "Позиция заказа"),
                controllerCase(mock(OrganizerRepository.class),    OrganizerApiController::new,    new Organizer(),   "Организатор"),
                controllerCase(mock(ReviewRepository.class),       ReviewApiController::new,       new Review(),      "Отзыв"),
                controllerCase(mock(RoleRepository.class),         RoleApiController::new,         new Role(),        "Роль"),
                controllerCase(mock(TicketRepository.class),       TicketApiController::new,       new Ticket(),      "Билет"),
                controllerCase(mock(UserRepository.class),         UserApiController::new,         new User(),        "Пользователь"),
                controllerCase(mock(UserLogRepository.class),      UserLogApiController::new,      new UserLog(),     "Связь пользователя и записи")
        );
    }

    @ParameterizedTest
    @MethodSource("controllers")
    <T> void findAllDelegatesToRepository(ControllerCase<T> controllerCase) {
        when(controllerCase.repository().findAll())
                .thenReturn(List.of(controllerCase.sampleEntity()));

        var result = controllerCase.controller().findAll();

        assertThat(result).containsExactly(controllerCase.sampleEntity());
        verify(controllerCase.repository()).findAll();
    }

    @ParameterizedTest
    @MethodSource("controllers")
    <T> void findByIdThrowsWhenMissing(ControllerCase<T> controllerCase) {
        when(controllerCase.repository().findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controllerCase.controller().findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(controllerCase.resourceName());
    }

    @ParameterizedTest
    @MethodSource("controllers")
    <T> void createResetsIdAndSaves(ControllerCase<T> controllerCase) {
        when(controllerCase.repository().save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        T created = controllerCase.controller().create(controllerCase.sampleEntity());

        verify(controllerCase.repository()).save(controllerCase.sampleEntity());
        assertThat(created).isEqualTo(controllerCase.sampleEntity());
    }

    @ParameterizedTest
    @MethodSource("controllers")
    <T> void deleteRemovesEntity(ControllerCase<T> controllerCase) {
        when(controllerCase.repository().findById(5L))
                .thenReturn(Optional.of(controllerCase.sampleEntity()));

        controllerCase.controller().delete(5L);

        verify(controllerCase.repository()).delete(controllerCase.sampleEntity());
    }

    @ParameterizedTest
    @MethodSource("controllers")
    <T> void updateSavesWhenExists(ControllerCase<T> controllerCase) {
        when(controllerCase.repository().existsById(7L)).thenReturn(true);
        when(controllerCase.repository().save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        controllerCase.controller().update(7L, controllerCase.sampleEntity());

        verify(controllerCase.repository()).save(controllerCase.sampleEntity());
    }

    @ParameterizedTest
    @MethodSource("controllers")
    <T> void updateThrowsWhenMissing(ControllerCase<T> controllerCase) {
        when(controllerCase.repository().existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> controllerCase.controller().update(9L, controllerCase.sampleEntity()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(controllerCase.resourceName());
    }

    @org.junit.jupiter.api.Test
    void eventControllerSetsTicketBackReference() {
        EventRepository repo = mock(EventRepository.class);
        EventApiController controller = new EventApiController(repo);
        Event event = new Event();
        Ticket ticket = new Ticket();
        event.setTickets(List.of(ticket));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.create(event);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTickets()).hasSize(1);
        assertThat(captor.getValue().getTickets().get(0).getEvent()).isEqualTo(event);
    }

    @org.junit.jupiter.api.Test
    void orderControllerSetsOrderOnItems() {
        OrderEntityRepository repo = mock(OrderEntityRepository.class);
        OrderApiController controller = new OrderApiController(repo);
        OrderEntity order = new OrderEntity();
        OrderItem item = new OrderItem();
        order.setItems(List.of(item));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.create(order);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
        assertThat(captor.getValue().getItems().get(0).getOrder()).isEqualTo(order);
    }
}
