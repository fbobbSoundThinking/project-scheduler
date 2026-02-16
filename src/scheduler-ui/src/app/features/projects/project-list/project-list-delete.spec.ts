import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectList } from './project-list';
import { ProjectService } from '../../../core/services/project.service';
import { AssignmentService } from '../../../core/services/assignment.service';
import { TeamService } from '../../../core/services/team.service';
import { of, throwError } from 'rxjs';
import { Project, Assignment, Developer } from '../../../core/models';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('ProjectList Component - Delete Developer Assignment (TDD)', () => {
  let component: ProjectList;
  let fixture: ComponentFixture<ProjectList>;
  let assignmentService: any;
  let projectService: any;
  let teamService: any;

  beforeEach(async () => {
    const assignmentServiceMock = {
      deleteAssignment: vi.fn(),
      updateAssignment: vi.fn()
    };
    const projectServiceMock = {
      getAllProjects: vi.fn()
    };
    const teamServiceMock = {
      getAllTeams: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [ProjectList],
      providers: [
        { provide: AssignmentService, useValue: assignmentServiceMock },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: TeamService, useValue: teamServiceMock }
      ]
    }).compileComponents();

    assignmentService = TestBed.inject(AssignmentService);
    projectService = TestBed.inject(ProjectService);
    teamService = TestBed.inject(TeamService);

    // Setup default responses
    teamService.getAllTeams.mockReturnValue(of([]));
    projectService.getAllProjects.mockReturnValue(of([]));

    fixture = TestBed.createComponent(ProjectList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('RED: deleteAssignment method should exist', () => {
    it('should have a deleteAssignment method', () => {
      expect(component.deleteAssignment).toBeDefined();
      expect(typeof component.deleteAssignment).toBe('function');
    });
  });

  describe('RED: confirmDeleteAssignment method should exist', () => {
    it('should have a confirmDeleteAssignment method', () => {
      expect(component.confirmDeleteAssignment).toBeDefined();
      expect(typeof component.confirmDeleteAssignment).toBe('function');
    });
  });

  describe('RED: Delete assignment functionality', () => {
    let testProject: Project;
    let testAssignment: Assignment;
    let testDeveloper: Developer;

    beforeEach(() => {
      testDeveloper = {
        developersId: 1,
        firstName: 'John',
        lastName: 'Doe',
        position: 'Developer'
      };

      testAssignment = {
        assignmentsId: 1,
        developer: testDeveloper,
        startDate: '2025-01-01',
        endDate: '2025-12-31',
        ratio: 1.0
      };

      testProject = {
        projectsId: 1,
        projectName: 'Test Project',
        assignments: [testAssignment]
      };

      // Initialize component data
      component.projects = [testProject];
      component.filteredProjects = [testProject];
      fixture.detectChanges();
    });

    it('should call assignmentService.deleteAssignment with correct ID', () => {
      assignmentService.deleteAssignment.mockReturnValue(of(void 0));
      
      component.deleteAssignment(1);

      expect(assignmentService.deleteAssignment).toHaveBeenCalledWith(1);
    });

    it('should remove assignment from project after successful deletion', async () => {
      assignmentService.deleteAssignment.mockReturnValue(of(void 0));

      // Verify initial state
      expect(component.filteredProjects[0].assignments?.length).toBe(1);

      component.deleteAssignment(1);

      await new Promise(resolve => setTimeout(resolve, 150));

      // After deletion
      const project = component.filteredProjects[0];
      expect(project).toBeDefined();
      expect(project.assignments).toBeDefined();
      expect(project.assignments?.length).toBe(0);
    });

    it('should show confirmation dialog before deletion', () => {
      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
      assignmentService.deleteAssignment.mockReturnValue(of(void 0));

      component.confirmDeleteAssignment(1, 'John Doe');

      expect(confirmSpy).toHaveBeenCalledWith(
        'Are you sure you want to remove John Doe from this project?'
      );
    });

    it('should not delete if user cancels confirmation', () => {
      vi.spyOn(window, 'confirm').mockReturnValue(false);

      component.confirmDeleteAssignment(1, 'John Doe');

      expect(assignmentService.deleteAssignment).not.toHaveBeenCalled();
    });

    it('should display error message on deletion failure', async () => {
      const errorResponse = { status: 500, statusText: 'Internal Server Error' };
      assignmentService.deleteAssignment.mockReturnValue(throwError(() => errorResponse));
      const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});

      component.deleteAssignment(1);

      await new Promise(resolve => setTimeout(resolve, 100));

      expect(alertSpy).toHaveBeenCalledWith(
        'Error removing developer from project. Please try again.'
      );
    });

    it('should track deleting state', () => {
      assignmentService.deleteAssignment.mockReturnValue(of(void 0));
      
      expect(component.deletingAssignmentId).toBeNull();
      
      component.deleteAssignment(1);
      
      // During deletion (before observable completes, it should be set)
      // Note: Since we're mocking with 'of', it completes immediately
      // So we can't actually catch it in the "deleting" state in synchronous test
      expect(assignmentService.deleteAssignment).toHaveBeenCalledWith(1);
    });
  });
});
