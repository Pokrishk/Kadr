package com.example.Kadr.controller.api;

import com.example.Kadr.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyDescriptor;
import java.util.List;

@Validated
public abstract class AbstractCrudApiController<T> {

    protected abstract JpaRepository<T, Long> getRepository();

    protected abstract String getResourceName();

    protected void prepareForCreate(T entity) {
    }

    protected void prepareForUpdate(Long id, T entity) {
        prepareForCreate(entity);
    }

    private void setEntityId(T entity, Long id) {
        PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(entity.getClass(), "id");
        if (descriptor != null && descriptor.getWriteMethod() != null) {
            try {
                descriptor.getWriteMethod().invoke(entity, id);
                return;
            } catch (Exception ex) {
                throw new IllegalStateException("Не удалось установить идентификатор для ресурса " + getResourceName(), ex);
            }
        }
        throw new IllegalStateException("Ресурс " + getResourceName() + " не поддерживает изменение идентификатора");
    }

    @GetMapping
    @Operation(summary = "Получить список записей", description = "Возвращает все записи ресурса")
    public List<T> findAll() {
        return getRepository().findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по идентификатору")
    public T findById(@Parameter(description = "Идентификатор записи") @PathVariable Long id) {
        return getRepository()
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getResourceName(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать новую запись")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Запись создана",
                    content = @Content(schema = @Schema(implementation = Object.class)))
    })
    public T create(@Valid @RequestBody T entity) {
        prepareForCreate(entity);
        setEntityId(entity, null);
        return getRepository().save(entity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить существующую запись")
    public T update(@Parameter(description = "Идентификатор записи") @PathVariable Long id,
                    @Valid @RequestBody T entity) {
        if (!getRepository().existsById(id)) {
            throw new ResourceNotFoundException(getResourceName(), id);
        }
        prepareForUpdate(id, entity);
        setEntityId(entity, id);
        return getRepository().save(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Запись удалена", content = @Content),
            @ApiResponse(responseCode = "404", description = "Запись не найдена", content = @Content)
    })
    public ResponseEntity<Void> delete(@Parameter(description = "Идентификатор записи") @PathVariable Long id) {
        T entity = getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getResourceName(), id));
        getRepository().delete(entity);
        return ResponseEntity.noContent().build();
    }
}