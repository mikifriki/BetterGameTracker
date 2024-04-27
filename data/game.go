package data

type GameEntry struct {
	Title   string
	Details VideoGameDetails
}

type VideoGameDetails struct {
	Description        string
	ReleasePlatform    string
	ReleaseDate        string
	Developer          string
	MetaRating         string
	MyMainRating       string
	PhysicalCopy       string
	CoverImageLocation string
	UserDataLocation   string
}

type PlayTime struct {
	MyRating         string
	CompletionDate   string
	PlatformPlayedOn string
	TimeToBeat       string
	CompletionRate   string
	Coop             string
	Location         string
	ReviewSection    Review
}

type Review struct {
	ReviewDate  string
	ReviewTitle string
	Review      string
	Screenshots []Image
	Rating      string
}

type Image struct {
	Location string
	Title    string
}

func CreateGame(title string) *GameEntry {
	game := GameEntry{
		Title: title,
		Details: VideoGameDetails{
			Description: "batt2le",
		},
	}
	return &game
}
