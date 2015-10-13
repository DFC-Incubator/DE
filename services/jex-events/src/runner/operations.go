package main

import (
	"api"
	"log"
	"model"
	"os"
	"os/exec"
)

func createLogsDir() {
	err := os.Mkdir("logs", 0755)
	if err != nil {
		logger.Fatal(err)
	}
}

func moveIplantCmd() {
	if _, err := os.Stat("iplant.cmd"); err != nil {
		if err = os.Rename("iplant.cmd", "logs/iplant.cmd"); err != nil {
			logger.Print(err)
		}
	}
}

func createTransferTrigger() {
	transferTrigger, err := os.Create("logs/de-transfer-trigger.log")
	if err != nil {
		logger.Print(err)
	} else {
		_, err = transferTrigger.WriteString("This is only used to force HTCondor to transfer files.")
		if err != nil {
			logger.Print(err)
		}
	}
}

func pullDataContainers(job *model.Job) api.StatusCode {
	var err error
	status := api.Success
	for _, dc := range job.DataContainers() {
		cmd := exec.Command("docker", DataContainerPullArgs(&dc)...)
		err = cmd.Run()
		if err != nil {
			log.Print(err)
			status = api.StatusDockerPullFailed
			break
		}
		if status == api.Success {
			cmd = exec.Command("docker", DataContainerCreateArgs(&dc, job.InvocationID)...)
			err = cmd.Run()
			if err != nil {
				log.Print(err)
				status = api.StatusDockerCreateFailed
				break
			}
		}
	}
	return status
}

func pullContainerImages(job *model.Job) api.StatusCode {
	var err error
	status := api.Success
	for _, ci := range job.ContainerImages() {
		cmd := exec.Command("docker", ContainerImagePullArgs(&ci)...)
		err = cmd.Run()
		if err != nil {
			log.Print(err)
			status = api.StatusDockerPullFailed
			break
		}
	}
	return status
}

func transferInputs(job *model.Job) api.StatusCode {
	status := api.Success
	for _, input := range job.Inputs() {
		cmd := exec.Command("docker", input.Arguments(job.Submitter, job.InvocationID, job.FileMetadata)...)
		stdout, err := os.Open(input.Stdout(job.InvocationID))
		if err != nil {
			log.Print(err)
		} else {
			cmd.Stdout = stdout
		}
		stderr, err := os.Open(input.Stderr(job.InvocationID))
		if err != nil {
			log.Print(err)
		} else {
			cmd.Stderr = stderr
		}
		err = cmd.Run()
		if err != nil {
			log.Print(err)
			status = api.StatusInputFailed
			break
		}
	}
	return status
}

func executeStep(job *model.Job, step *model.Step) api.StatusCode {
	status := api.Success
	cmd := exec.Command("docker", step.Arguments(job.InvocationID)...)
	stdout, err := os.Open(step.Stdout(job.InvocationID))
	if err != nil {
		log.Print(err)
	} else {
		cmd.Stdout = stdout
	}
	stderr, err := os.Open(step.Stderr(job.InvocationID))
	if err != nil {
		log.Print(err)
	} else {
		cmd.Stderr = stderr
	}
	cmd.Env = Environment(job)
	err = cmd.Run()
	if err != nil {
		log.Print(err)
		status = api.StatusStepFailed
	}
	return status
}

func transferOutputs(job *model.Job) api.StatusCode {
	status := api.Success
	cmd := exec.Command("docker", job.FinalOutputArguments()...)
	stdout, err := os.Open("logs/logs-stdout-output")
	if err != nil {
		log.Print(err)
	} else {
		cmd.Stdout = stdout
	}
	stderr, err := os.Open("logs/logs-stderr-output")
	if err != nil {
		log.Print(err)
	} else {
		cmd.Stderr = stderr
	}
	err = cmd.Run()
	if err != nil {
		log.Print(err)
		status = api.StatusOutputFailed
	}
	return status
}