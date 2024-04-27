package db

import (
	"encoding/json"
	"fmt"
	"gametracker/data"
	"log"
	"os"
)

func CreateNew(g []data.GameEntry) {
	// encode to json object

	file, err := os.Create("testData/" + "db" + ".json")
	if err != nil {
		fmt.Println(err)
	}
	defer file.Close()
	encoder := json.NewEncoder(file)
	encoder.Encode(g)
}

func ReadFile(jsonLocation string) []data.GameEntry {
	content, err := os.ReadFile(jsonLocation)
	if err != nil {
		log.Println(err)
	}
	fmt.Println("Successfully Opened users.json")

	games := []data.GameEntry{}

	json.Unmarshal(content, &games)
	return games
}
