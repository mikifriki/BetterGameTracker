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
	db.CheckAndCreateDir(util.GlobalConfig.CoverImageDirectory)

	// Start web server
	router := gin.Default()
	// Select the default API for the backend.
	database := defaultAPI(util.GlobalConfig.DefaultDB)
	router.Use(CORSMiddleware())
	router.GET("/getAllGames", database.GetGames)
	router.GET("/getGameEntry", database.GetGameData)
	router.POST("/newGameEntry", database.CreateNewGameEntry)
	router.POST("/uploadCoverImage", database.UploadCoverImage)
	router.GET("/getCoverImage", database.GetCoverImage)
	router.PUT("/updateGameEntry", database.UpdateGameEntry)
	router.POST("/addPlayEntry", database.AddPlayEntry)
	router.PUT("/updatePlayEntry", database.UpdatePlayEntry)
	err := router.Run()
	if err != nil {
		log.Println("Startup failed with error: " + err.Error())
		return
	}
	//err := router.RunTLS(":"+util.GlobalConfig.ServerPort, util.GlobalConfig.PathToCertificate, util.GlobalConfig.PathToPrivatekey)
	//if err != nil {
	//	log.Println(err)
	// }
}

func defaultAPI(dbType string) api.Model {
	switch dbType {
	case "db":
		return api.DBApi{}
	default:
		return api.JSONApi{}
	}
}

// CORSMiddleware Not safe and should not be used. This is for testing only
func CORSMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
		c.Writer.Header().Set("Access-Control-Allow-Credentials", "true")
		c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization, accept, origin, Cache-Control, X-Requested-With")
		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS, GET, PUT")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}

		c.Next()
	}
}
