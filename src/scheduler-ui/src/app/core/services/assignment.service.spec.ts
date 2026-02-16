import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AssignmentService } from './assignment.service';
import { Assignment } from '../models';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('AssignmentService - Delete Operations', () => {
  let service: AssignmentService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8080/api/assignments';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AssignmentService]
    });
    service = TestBed.inject(AssignmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call DELETE endpoint with correct ID', () => {
    const assignmentId = 1;

    service.deleteAssignment(assignmentId).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/${assignmentId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should handle successful deletion', () => {
    const assignmentId = 1;

    service.deleteAssignment(assignmentId).subscribe({
      next: (response) => {
        expect(response).toBeNull();
      },
      error: () => {
        throw new Error('Should not have errored');
      }
    });

    const req = httpMock.expectOne(`${apiUrl}/${assignmentId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 200, statusText: 'OK' });
  });

  it('should handle 404 error when assignment not found', () => {
    const assignmentId = 999;
    const errorMessage = 'Assignment not found';

    service.deleteAssignment(assignmentId).subscribe({
      next: () => {
        throw new Error('should have failed with 404 error');
      },
      error: (error) => {
        expect(error.status).toBe(404);
        expect(error.statusText).toBe('Not Found');
      }
    });

    const req = httpMock.expectOne(`${apiUrl}/${assignmentId}`);
    req.flush(errorMessage, { status: 404, statusText: 'Not Found' });
  });

  it('should handle server error (500) on deletion', () => {
    const assignmentId = 1;

    service.deleteAssignment(assignmentId).subscribe({
      next: () => {
        throw new Error('should have failed with 500 error');
      },
      error: (error) => {
        expect(error.status).toBe(500);
        expect(error.statusText).toBe('Internal Server Error');
      }
    });

    const req = httpMock.expectOne(`${apiUrl}/${assignmentId}`);
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
  });
});
