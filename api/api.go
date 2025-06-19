package api

import (
	"fmt"
	"gametracker/data"
	"gametracker/db"
	"gametracker/util"
	"net/http"
	"os"
	"strings"

	"github.com/gin-gonic/gin"
)

type JSONApi struct{}

// GetGames Returns a list of all games
func (JSONApi) GetGames(c *gin.Context) {
	games, err := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")
	if err != nil {
		c.IndentedJSON(http.StatusBadRequest, "No games found")
		return
	}

	c.IndentedJSON(http.StatusOK, games)
}

// CreateNewGameEntry Creates new Game entry
func (JSONApi) CreateNewGameEntry(c *gin.Context) {
	games, _ := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")
	var newGame data.GameEntry

	bindErr := c.BindJSON(&newGame)
	if bindErr != nil {
		c.IndentedJSON(http.StatusBadRequest, "Could not create JSON object")
		return
	}

	newGame.Id, _ = util.GenerateID()

	if !util.GameExists(games, newGame) {
		newGame.HrefTitle = strings.ReplaceAll(newGame.GameTitle, " ", "")
		*games = append(*games, newGame)
		db.CreateNewFile(*games, util.GlobalConfig.MainJsonDbDirectory+"db.json")
		db.CheckAndCreateDir(util.GlobalConfig.MainJsonDbDirectory + newGame.GameTitle)
		c.String(http.StatusOK, newGame.Id)
		return
	}

	c.String(http.StatusBadRequest, "Duplicate Entry")
}

// UpdateGameEntry Update game entry by id and title
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

	// If entry exists then replace the whole object
	for index, element := range *games {
		if element.Id == newGame.Id {
			(*games)[index] = newGame
			updatedEntry = true
			break
		}
	}

	if !updatedEntry {
		c.IndentedJSON(http.StatusBadRequest, "Failed to update game entry")
		return
	}

	db.CreateNewFile(*games, util.GlobalConfig.MainJsonDbDirectory+"db.json")
	c.IndentedJSON(http.StatusOK, newGame)
}

func (JSONApi) DeleteGameEntry(c *gin.Context) {
	games, readErr := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")
	entry := c.Param("id")
	if readErr != nil {
		c.IndentedJSON(http.StatusBadRequest, "No Game Entries found")
		return
	}

	// Get Element index in array
	var result []data.GameEntry
	var removedEntry data.GameEntry
	for _, element := range *games {
		if element.Id == entry {
			removedEntry = element
			continue
		}
		result = append(result, element)
	}

	// Error checks should be added here
	db.DeleteDir(util.GlobalConfig.MainJsonDbDirectory + removedEntry.GameTitle)
	db.DeleteFile(util.GlobalConfig.CoverImageDirectory + removedEntry.GameTitle + ".png")
	db.CreateNewFile(result, util.GlobalConfig.MainJsonDbDirectory+"db.json")

	c.IndentedJSON(http.StatusOK, removedEntry)
}

// UploadCoverImage Saves the uploaded cover image
func (JSONApi) UploadCoverImage(c *gin.Context) {
	file, _ := c.FormFile("file")
	gameName := c.PostForm("fileName")

	if file.Size > 4000000 {
		c.String(http.StatusBadRequest, fmt.Sprintf("Image too large. Please limit size below 4mb"))
	}

	if file != nil {
		openedFile, _ := file.Open()
		buff := make([]byte, 512)
		openedFile.Read(buff)
		fileDetails := http.DetectContentType(buff)
		if fileDetails != "image/jpeg" && fileDetails != "image/png" {
			c.String(http.StatusBadRequest, fmt.Sprintf("Image is not in jpeg or png format. Please reformat image"))
		}
	}

	// Forces the image to be in png format but should work fine for most images
	if file == nil {
		c.String(http.StatusBadRequest, fmt.Sprintf("Upload failed due to missing file"))
	}

	name := strings.ReplaceAll(gameName, " ", "")
	file.Filename = name + ".png"

	c.SaveUploadedFile(file, util.GlobalConfig.CoverImageDirectory+file.Filename)
	c.String(http.StatusOK, fmt.Sprintf("'%s' uploaded!", file.Filename))
}

// GetCoverImage Get the cover image for the given game
func (JSONApi) GetCoverImage(c *gin.Context) {
	// Upload the file to specific dst.
	file, _ := os.ReadFile(util.GlobalConfig.CoverImageDirectory + c.Query("Cover") + ".png")

	c.Header("Content-Disposition", "inline; filename=Drakan1.png")
	c.Data(http.StatusOK, "image/png", file)
}

// GetGameData Get single game info from the db
func (JSONApi) GetGameData(c *gin.Context) {
	games, _ := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")

	for _, element := range *games {
		if element.GameTitle == c.Query("GameTitle") {
			c.IndentedJSON(http.StatusOK, element)
			return
		}
	}

	c.String(http.StatusBadRequest, "No Game found")
}

// AddPlayEntry Creates new play entry in the game directory.
func (JSONApi) AddPlayEntry(c *gin.Context) {
	var newEntry data.PlayEntry
	err := c.BindJSON(&newEntry)
	if err != nil {
		c.IndentedJSON(http.StatusBadRequest, "Could not create JSON object"+err.Error())
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

// UpdatePlayEntry Updates a play entry.
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
	if readErr == nil {
		for index, entry := range *existingEntries {
			if entry.Id == existingEntry.Details.Id {
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
