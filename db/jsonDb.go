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
	// encode to json object

	file, err := os.Create(fullLocation)
	if err != nil {
		fmt.Println(err)
	}

	defer file.Close()
	encoder := json.NewEncoder(file)
	encoder.Encode(jsonData)
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
