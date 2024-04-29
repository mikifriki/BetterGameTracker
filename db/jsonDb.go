package db

import (
	"encoding/json"
	"fmt"
	"gametracker/data"
	"log"
	"os"
)

type structTypes interface {
	[]data.GameEntry | []data.PlayEntry
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

func ReadGameEntry(jsonLocation string) []data.GameEntry {
	content, err := os.ReadFile(jsonLocation)
	if err != nil {
		log.Println(err)
	}

	games := []data.GameEntry{}
	json.Unmarshal(content, &games)

	return games
}

func ReadPlayEntry(jsonLocation string) []data.PlayEntry {
	content, err := os.ReadFile(jsonLocation)
	if err != nil {
		log.Println(err)
	}

	allEntries := []data.PlayEntry{}
	json.Unmarshal(content, &allEntries)

	return allEntries
}

func CheckAndCreateDir(dirName string) {
	err := os.Mkdir("testData/"+dirName, os.ModePerm)
	if err == nil {
		log.Println(err)
	}
}
