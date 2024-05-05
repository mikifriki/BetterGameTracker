package main

import (
	"gametracker/api"
	"gametracker/db"
	"gametracker/util"
	"log"

	"github.com/gin-gonic/gin"
)

func main() {
	config := util.Config{}

	confErr := config.ReadConfigFile()
	if confErr != nil {
		log.Fatalf("Failed to read config file")
		return
	}

	if util.GlobalConfig.ServerPort == "" {
		log.Fatalf("Please provide config file")
		return
	}
	// Create default directories.
	db.CheckAndCreateDir(util.GlobalConfig.MainJsonDbDirectory)

	// Start web server
	router := gin.Default()
	// Select the default API for the backend.
	db := defaultAPI(util.GlobalConfig.DefaultDB)

	router.GET("/getAllGames", db.GetGames)
	router.POST("/newGameEntry", db.CreateNewGameEntry)
	router.PUT("/updateGameEntry", db.UpdateGameEntry)
	router.POST("/addPlayEntry", db.AddPlayEntry)
	router.PUT("/updatePlayEntry", db.UpdatePlayEntry)

	err := router.RunTLS(":"+util.GlobalConfig.ServerPort, util.GlobalConfig.PathToCertificate, util.GlobalConfig.PathToPrivatekey)
	if err != nil {
		log.Println(err)
	}
}

func defaultAPI(dbType string) api.ApiModel {
	switch dbType {
	case "db":
		return api.DBApi{}
	default:
		return api.JSONApi{}
	}
}
