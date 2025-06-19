package db

import (
	"encoding/json"
	"fmt"
	"gametracker/data"
	"log"
	"os"
)

type structTypes interface {
	[]data.GameEntry | []data.GameEntryDetails
}

func CreateNewFile[T structTypes](jsonData T, fullLocation string) {
	file, err := os.Create(fullLocation)
	if err != nil {
		fmt.Println(err)
	}

	encoder := json.NewEncoder(file)
	encoderErr := encoder.Encode(jsonData)
	if encoderErr != nil {
		return
	}
	fileErr := file.Close()
	if fileErr != nil {
		return
	}
}

func ReadGameEntries(jsonLocation string) (*[]data.GameEntry, error) {
	games := &[]data.GameEntry{}
	content, err := os.ReadFile(jsonLocation)
	if err != nil {
		return games, err
	}

	json.Unmarshal(content, &games)

	return games, nil
}

func ReadPlayEntries(jsonLocation string) (*[]data.GameEntryDetails, error) {
	allEntries := &[]data.GameEntryDetails{}
	content, err := os.ReadFile(jsonLocation)
	if err != nil {
		return allEntries, err
	}

	json.Unmarshal(content, &allEntries)

	return allEntries, nil
}

func CheckAndCreateDir(dir string) {
	err := os.Mkdir(dir, os.ModePerm)
	if err == nil {
		log.Println(err)
	}
}

func DeleteDir(dir string) {
	err := os.RemoveAll(dir)
	if err == nil {

		log.Println(err)
	}
}

func DeleteFile(dir string) {
	err := os.Remove(dir)
	if err == nil {
		log.Println(err)
	}
}
