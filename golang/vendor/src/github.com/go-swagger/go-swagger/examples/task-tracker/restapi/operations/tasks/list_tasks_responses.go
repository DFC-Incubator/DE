package tasks

// This file was generated by the swagger tool.
// Editing this file might prove futile when you re-run the swagger generate command

import (
	"fmt"
	"net/http"

	"github.com/go-swagger/go-swagger/httpkit"

	"github.com/go-swagger/go-swagger/examples/task-tracker/models"
)

/*ListTasksOK Successful response

swagger:response listTasksOK
*/
type ListTasksOK struct {
	/*the last task id known to the application
	  Required: true
	*/
	XLastTaskID int64 `json:"X-Last-Task-Id"`

	// In: body
	Payload []*models.TaskCard `json:"body,omitempty"`
}

// NewListTasksOK creates ListTasksOK with default headers values
func NewListTasksOK() *ListTasksOK {
	return &ListTasksOK{}
}

// WithXLastTaskID adds the xLastTaskId to the list tasks o k response
func (o *ListTasksOK) WithXLastTaskID(xLastTaskID int64) *ListTasksOK {
	o.XLastTaskID = xLastTaskID
	return o
}

// SetXLastTaskID sets the xLastTaskId to the list tasks o k response
func (o *ListTasksOK) SetXLastTaskID(xLastTaskID int64) {
	o.XLastTaskID = xLastTaskID
}

// WithPayload adds the payload to the list tasks o k response
func (o *ListTasksOK) WithPayload(payload []*models.TaskCard) *ListTasksOK {
	o.Payload = payload
	return o
}

// SetPayload sets the payload to the list tasks o k response
func (o *ListTasksOK) SetPayload(payload []*models.TaskCard) {
	o.Payload = payload
}

// WriteResponse to the client
func (o *ListTasksOK) WriteResponse(rw http.ResponseWriter, producer httpkit.Producer) {

	// response header X-Last-Task-Id
	rw.Header().Add("X-Last-Task-Id", fmt.Sprintf("%v", o.XLastTaskID))

	rw.WriteHeader(200)
	if err := producer.Produce(rw, o.Payload); err != nil {
		panic(err) // let the recovery middleware deal with this
	}

}

/*ListTasksUnprocessableEntity Validation error

swagger:response listTasksUnprocessableEntity
*/
type ListTasksUnprocessableEntity struct {

	// In: body
	Payload *models.ValidationError `json:"body,omitempty"`
}

// NewListTasksUnprocessableEntity creates ListTasksUnprocessableEntity with default headers values
func NewListTasksUnprocessableEntity() *ListTasksUnprocessableEntity {
	return &ListTasksUnprocessableEntity{}
}

// WithPayload adds the payload to the list tasks unprocessable entity response
func (o *ListTasksUnprocessableEntity) WithPayload(payload *models.ValidationError) *ListTasksUnprocessableEntity {
	o.Payload = payload
	return o
}

// SetPayload sets the payload to the list tasks unprocessable entity response
func (o *ListTasksUnprocessableEntity) SetPayload(payload *models.ValidationError) {
	o.Payload = payload
}

// WriteResponse to the client
func (o *ListTasksUnprocessableEntity) WriteResponse(rw http.ResponseWriter, producer httpkit.Producer) {

	rw.WriteHeader(422)
	if o.Payload != nil {
		if err := producer.Produce(rw, o.Payload); err != nil {
			panic(err) // let the recovery middleware deal with this
		}
	}
}

/*ListTasksDefault Error response

swagger:response listTasksDefault
*/
type ListTasksDefault struct {
	_statusCode int
	/*
	  Required: true
	*/
	XErrorCode string `json:"X-Error-Code"`

	// In: body
	Payload *models.Error `json:"body,omitempty"`
}

// NewListTasksDefault creates ListTasksDefault with default headers values
func NewListTasksDefault(code int) *ListTasksDefault {
	if code <= 0 {
		code = 500
	}

	return &ListTasksDefault{
		_statusCode: code,
	}
}

// WithStatusCode adds the status to the list tasks default response
func (o *ListTasksDefault) WithStatusCode(code int) *ListTasksDefault {
	o._statusCode = code
	return o
}

// SetStatusCode sets the status to the list tasks default response
func (o *ListTasksDefault) SetStatusCode(code int) {
	o._statusCode = code
}

// WithXErrorCode adds the xErrorCode to the list tasks default response
func (o *ListTasksDefault) WithXErrorCode(xErrorCode string) *ListTasksDefault {
	o.XErrorCode = xErrorCode
	return o
}

// SetXErrorCode sets the xErrorCode to the list tasks default response
func (o *ListTasksDefault) SetXErrorCode(xErrorCode string) {
	o.XErrorCode = xErrorCode
}

// WithPayload adds the payload to the list tasks default response
func (o *ListTasksDefault) WithPayload(payload *models.Error) *ListTasksDefault {
	o.Payload = payload
	return o
}

// SetPayload sets the payload to the list tasks default response
func (o *ListTasksDefault) SetPayload(payload *models.Error) {
	o.Payload = payload
}

// WriteResponse to the client
func (o *ListTasksDefault) WriteResponse(rw http.ResponseWriter, producer httpkit.Producer) {

	// response header X-Error-Code
	rw.Header().Add("X-Error-Code", fmt.Sprintf("%v", o.XErrorCode))

	rw.WriteHeader(o._statusCode)
	if o.Payload != nil {
		if err := producer.Produce(rw, o.Payload); err != nil {
			panic(err) // let the recovery middleware deal with this
		}
	}
}
