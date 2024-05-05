package main

import (
	"gametracker/api"
	"log"

	"github.com/gin-gonic/gin"
)

func main() {

	router := gin.Default()

	db := setupProperApi("json")
	router.GET("/getAllGames", db.GetGames)
	router.POST("/newGameEntry", db.CreateNewGameEntry)
	router.PUT("/updateGameEntry", db.UpdateGameEntry)
	router.POST("/addPlayEntry", db.AddPlayEntry)
	router.PUT("/updatePlayEntry", db.UpdatePlayEntry)

	err := router.RunTLS(":8080", "certificate.pem", "private.key")
	if err != nil {
		log.Println(err)
	}
}

func setupProperApi(dbType string) api.ApiModel {
	switch dbType {
	case "db":
		return api.DBApi{}
	default:
		return api.JSONApi{}
	}
}
