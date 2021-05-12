package org.hl7.gravity.refimpl.sdohexchange.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.common.base.Strings;
import com.healthlx.smartonfhir.core.SmartOnFhirContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.codesystems.EndpointConnectionType;
import org.hl7.gravity.refimpl.sdohexchange.codesystems.OrganizationTypeCode;
import org.hl7.gravity.refimpl.sdohexchange.dao.impl.OrganizationRepository;
import org.hl7.gravity.refimpl.sdohexchange.dao.impl.TaskRepository;
import org.hl7.gravity.refimpl.sdohexchange.dto.converter.info.TaskInfoToDtoConverter;
import org.hl7.gravity.refimpl.sdohexchange.dto.request.NewTaskRequestDto;
import org.hl7.gravity.refimpl.sdohexchange.dto.request.UpdateTaskRequestDto;
import org.hl7.gravity.refimpl.sdohexchange.dto.response.TaskDto;
import org.hl7.gravity.refimpl.sdohexchange.dto.response.UserDto;
import org.hl7.gravity.refimpl.sdohexchange.fhir.factory.TaskBundleFactory;
import org.hl7.gravity.refimpl.sdohexchange.info.TaskInfo;
import org.hl7.gravity.refimpl.sdohexchange.info.composer.TasksInfoComposer;
import org.hl7.gravity.refimpl.sdohexchange.util.FhirUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class TaskService {

  private final IGenericClient ehrClient;
  private final FhirContext fhirContext;
  private final SmartOnFhirContext smartOnFhirContext;
  private final CbroTaskCreateService cbroTaskCreateService;
  private final PractitionerRoleService practitionerRoleService;
  private final ContextService contextService;
  private final TaskRepository taskRepository;
  private final OrganizationRepository organizationRepository;
  private final TasksInfoComposer tasksInfoComposer;

  public String newTask(NewTaskRequestDto taskRequest) {
    if (taskRequest.getConsent() != Boolean.TRUE) {
      throw new IllegalStateException("Patient consent must be provided. Set consent to TRUE.");
    }
    // Create a Task Bundle
    UserDto user = contextService.getUser();
    PractitionerRole role = practitionerRoleService.getRole(user.getId());
    String requesterId = role.getOrganization()
        .getReferenceElement()
        .getIdPart();

    TaskBundleFactory taskBundleFactory = new TaskBundleFactory(taskRequest.getName(), smartOnFhirContext.getPatient(),
        taskRequest.getCategory(), taskRequest.getRequest(), taskRequest.getPriority(), taskRequest.getOccurrence(),
        taskRequest.getPerformerId(), requesterId);
    taskBundleFactory.setComment(taskRequest.getComment());
    taskBundleFactory.setUser(user);

    // TODO check conditions and goals are of SDOHCC profile?
    if (taskRequest.getConditionIds() != null) {
      taskBundleFactory.getConditionIds()
          .addAll(taskRequest.getConditionIds());
    }
    if (taskRequest.getGoalIds() != null) {
      taskBundleFactory.getGoalIds()
          .addAll(taskRequest.getGoalIds());
    }

    // Verify References
    // Performer Id is set - assert is present in TaskBundleFactory.
    // Fetch it and related Endpoint in case an Organization is a CBRO.
    Bundle bundle = organizationRepository.find(taskRequest.getPerformerId(),
        Arrays.asList(Organization.INCLUDE_ENDPOINT));
    List<Organization> orgs = FhirUtil.getFromBundle(bundle, Organization.class);
    if (orgs.size() == 0) {
      throw new IllegalStateException("Organization with id '" + taskRequest.getPerformerId() + "' does not exist.");
    }

    Organization org = orgs.get(0);
    Endpoint endpoint = null;
    //TODO valdiate Organization using InstanceValidator. This will validate Organization type as well.
    Coding orgCoding = FhirUtil.findCoding(org.getType(), OrganizationTypeCode.SYSTEM);
    if (orgCoding != null && OrganizationTypeCode.CBRO.toCode()
        .equals(orgCoding.getCode())) {
      // Retrieve FHIR Endpoint instance
      endpoint = FhirUtil.getFromBundle(bundle, Endpoint.class)
          .stream()
          .filter(e -> e.getConnectionType()
              .getCode()
              .equals(EndpointConnectionType.HL7FHIRREST.toCode()))
          .findFirst()
          .orElse(null);

      if (endpoint == null) {
        throw new IllegalStateException(
            String.format("CBRO Organization resource with id '%s' does not contain endpoint of type '%s'.",
                org.getIdElement()
                    .getIdPart(), EndpointConnectionType.HL7FHIRREST));
      } else {
        if (Strings.isNullOrEmpty(endpoint.getAddress())) {
          throw new IllegalStateException(
              String.format("Endpoint resource with id '%s' for a CBRO organization '' does not contain an address.",
                  endpoint.getIdElement()
                      .getIdPart(), org.getIdElement()
                      .getIdPart()));
        }
      }
    }

    // Store a task
    Bundle taskCreateBundle = taskBundleFactory.createBundle();
    Bundle respBundle = ehrClient.transaction()
        .withBundle(taskCreateBundle)
        .execute();

    IdType taskId = FhirUtil.getFromResponseBundle(respBundle, Task.class);

    //If endpoint!=null - this is a CBRO use case. Manage a task additionally.
    if (endpoint != null) {
      Optional<Task> task = taskRepository.find(taskId.getIdPart());
      if(task.isPresent()){
        handleCbroTask(task.get(), endpoint);
      } else {
        throw new ResourceNotFoundException(taskId);
      }
    }
    return taskId.getIdPart();
  }

  public List<TaskDto> listTasks() {
    Assert.notNull(smartOnFhirContext.getPatient(), "Patient id cannot be null.");
    TaskInfoToDtoConverter taskInfoToDtoConverter = new TaskInfoToDtoConverter();
    Bundle bundle = taskRepository.findByPatientId(smartOnFhirContext.getPatient());
    List<TaskInfo> taskInfos = tasksInfoComposer.compose(bundle);
    return taskInfos.stream()
        .map(taskInfo -> taskInfoToDtoConverter.convert(taskInfo))
        .collect(Collectors.toList());
  }

  public TaskDto updateTask(String taskId, UpdateTaskRequestDto updateTaskDto) {
    Optional<Task> foundTask = taskRepository.find(taskId);
    if(!foundTask.isPresent()){
      throw new ResourceNotFoundException(new IdType(taskId));
    }
    Task task = foundTask.get();
    if (updateTaskDto.getStatus() != null && !Objects.equals(updateTaskDto.getStatus().getValue(), task.getStatus())) {
      task.setStatus(updateTaskDto.getStatus().getValue());
    }
    if (!Strings.isNullOrEmpty(updateTaskDto.getComment())) {
      UserDto user = contextService.getUser();
      task.addNote()
          .setText(updateTaskDto.getComment())
          .setTimeElement(DateTimeType.now())
          .setAuthor(new Reference(new IdType(user.getUserType(), user.getId())).setDisplay(user.getName()));
    }
    MethodOutcome methodOutcome = ehrClient.update().resource(task).execute();
    Bundle updatedTask = taskRepository.find(methodOutcome.getResource().getIdElement()
        .getIdPart(), Arrays.asList(Task.INCLUDE_FOCUS, Task.INCLUDE_OWNER));
    TaskInfo taskInfo = tasksInfoComposer.compose(updatedTask).get(0);
    return new TaskInfoToDtoConverter().convert(taskInfo);
  }

  protected void handleCbroTask(Task task, Endpoint endpoint) {
    try {
      // Create task in CBRO.
      cbroTaskCreateService.createTask(fhirContext.newRestfulGenericClient(endpoint.getAddress()), task);
      //TODO: Change task to received after CBRO receive it
      //      b.addEntry(FhirUtil.createPutEntry(task.setStatus(Task.TaskStatus.RECEIVED)
      //          .setLastModified(new Date())));
    } catch (CbroTaskCreateService.CbroTaskCreateException exc) {
      Bundle b = new Bundle();
      b.setType(Bundle.BundleType.TRANSACTION);
      log.warn(String.format("Task '%s' creation failed at CBRO. Failing a local Task and related ServiceRequest.",
          task.getIdElement()
              .getId()), exc);
      // If Task creation failed in CBRO - set local Task.status to FAILED and ServiceRequest status to REVOKED.
      // Additionally update a ServiceRequest status, but first - Read it.
      ServiceRequest serviceRequest = ehrClient.read()
          .resource(ServiceRequest.class)
          .withId(task.getFocus()
              .getReferenceElement())
          .execute();
      b.addEntry(FhirUtil.createPutEntry(task.setStatus(Task.TaskStatus.FAILED)
          .setLastModified(new Date())
          .setStatusReason(new CodeableConcept().setText(exc.getMessage()))));
      b.addEntry(FhirUtil.createPutEntry(serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.REVOKED)));
      ehrClient.transaction()
          .withBundle(b)
          .execute();
    }
  }
}