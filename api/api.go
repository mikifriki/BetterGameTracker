package api

import (
	"fmt"
	"gametracker/data"
	"gametracker/db"
	"gametracker/util"
	"log"
	"net/http"
	"os"
	"strings"

	"github.com/gin-gonic/gin"
)

type JSONApi struct{}

// Returns all current entries from db.json
func (JSONApi) GetGames(c *gin.Context) {
	games, err := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")
	if err != nil {
		c.IndentedJSON(http.StatusBadRequest, "No games found")
		return
	}

	c.IndentedJSON(http.StatusOK, games)
}

// Creates new Game entry in the db.json
func (JSONApi) CreateNewGameEntry(c *gin.Context) {
	games, _ := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")
	var newGame data.GameEntry

	bindErr := c.BindJSON(&newGame)
	if bindErr != nil {
		c.IndentedJSON(http.StatusBadRequest, "Could not create JSON object")
		return
	}

	// Check if game exists
	if !util.GameExists(games, newGame) {
		newGame.HrefTitle = strings.ReplaceAll(newGame.GameTitle, " ", "")
		*games = append(*games, newGame)
		db.CreateNewFile(*games, util.GlobalConfig.MainJsonDbDirectory+"db.json")
		db.CheckAndCreateDir(util.GlobalConfig.MainJsonDbDirectory + newGame.GameTitle)
		c.IndentedJSON(http.StatusOK, games)
		return
	}

	c.String(http.StatusBadRequest, "Duplicate Entry")
}

// Get single game info from the db
func (JSONApi) GetGameData(c *gin.Context) {
	games, _ := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")

	// If entry exists then update the details.
	for _, element := range *games {
		if element.GameTitle == c.Query("GameTitle") {
			c.IndentedJSON(http.StatusOK, element)
			return
		}
	}

	c.String(http.StatusBadRequest, "Duplicate Entry")

}

// Creates new Game entry in the db.json
func (JSONApi) UploadCoverImage(c *gin.Context) {

	// Check file size
	// Check that file is an image
	// Check which game the file should be assosiated to
	// single file
	file, _ := c.FormFile("file")
	name := strings.ReplaceAll(c.PostForm("GameTitle"), " ", "")
	log.Println(name)

	// Forces the image to be in png format but should work fine for most images
	file.Filename = name + ".png"
	// Upload the file to specific dst.
	c.SaveUploadedFile(file, util.GlobalConfig.CoverImageDirectory+file.Filename)

	c.String(http.StatusOK, fmt.Sprintf("'%s' uploaded!", file.Filename))
}

// Creates new Game entry in the db.json
func (JSONApi) GetCoverImage(c *gin.Context) {

	// Upload the file to specific dst.
	file, _ := os.ReadFile(util.GlobalConfig.CoverImageDirectory + c.Query("Cover") + ".png")

	c.Header("Content-Disposition", "inline; filename=Drakan1.png")
	c.Data(http.StatusOK, "image/png", file)

}

// Update game entry in db.json
func (JSONApi) UpdateGameEntry(c *gin.Context) {
	games, readErr := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")
	if readErr != nil {
		c.IndentedJSON(http.StatusBadRequest, "No Game Entries found")
		return
	}

	var newGame data.GameEntry
	updatedEntry := false
	err := c.BindJSON(&newGame)
	if err != nil {
		c.IndentedJSON(http.StatusBadRequest, "Could not create JSON object")
		return
	}

	// If entry exists then update the details.
	for index, element := range *games {
		if element.GameTitle == newGame.GameTitle {
			(*games)[index].Details = newGame.Details
			updatedEntry = true
			break
		}
	}

	if !updatedEntry {
		c.IndentedJSON(http.StatusBadRequest, "Failed to update game entry")
		return
	}

	// Write to new file
	db.CreateNewFile(*games, util.GlobalConfig.MainJsonDbDirectory+"db.json")
	c.IndentedJSON(http.StatusOK, newGame)
}

// Creates new play entry in the game directory.
func (JSONApi) AddPlayEntry(c *gin.Context) {
	var newEntry data.PlayEntry
	err := c.BindJSON(&newEntry)
	if err != nil {
		c.IndentedJSON(http.StatusBadRequest, "Could not create JSON object")
		return
	}

	existingEntries, readErr := db.ReadPlayEntries(util.GlobalConfig.MainJsonDbDirectory + "/" + newEntry.GameTitle + "/details.json")

	if readErr != nil {
		*existingEntries = append(*existingEntries, newEntry.Details)
		db.CreateNewFile(*existingEntries, util.GlobalConfig.MainJsonDbDirectory+newEntry.GameTitle+"/details.json")
		c.IndentedJSON(http.StatusOK, existingEntries)
		return
	}

	for _, entry := range *existingEntries {
		if entry.Id == newEntry.Details.Id {
			c.IndentedJSON(http.StatusBadRequest, "existingEntries")
			return
		}
	}

}

// Updates a play entry.
func (JSONApi) UpdatePlayEntry(c *gin.Context) {
	var existingEntry data.PlayEntry
	err := c.BindJSON(&existingEntry)
	if err != nil {
		c.IndentedJSON(http.StatusBadRequest, "Could not create JSON object")
		return
	}

	existingEntries, readErr := db.ReadPlayEntries(util.GlobalConfig.MainJsonDbDirectory + existingEntry.GameTitle + "/details.json")
	changedEntry := false

	// Add checks to only edit changed fields in the json.
	// Currently if the user only send half of the data then rest of it is lost.
	if readErr == nil {
		for index, entry := range *existingEntries {
			if entry.Id == existingEntry.Details.Id {
				// Update the array entry by index. This is a hard reset and will cause a reset if not all data is sent.
				(*existingEntries)[index] = existingEntry.Details
				changedEntry = true
				break
			}
		}
	} else {
		c.IndentedJSON(http.StatusBadRequest, "No play entries found")
		return
	}
	if !changedEntry {
		c.IndentedJSON(http.StatusBadRequest, "No play entries updated")
		return
	}
	db.CreateNewFile(*existingEntries, util.GlobalConfig.MainJsonDbDirectory+existingEntry.GameTitle+"/details.json")
	c.IndentedJSON(http.StatusOK, existingEntries)
}
