# GDrive - Cloud Storage Backend

This repository contains the backend service for GDrive, a cloud storage platform. It is built with Spring Boot and uses MinIO for S3-compatible object storage and JWT for secure, stateless authentication.

---

## 🚀 Features

* **Secure User Authentication:** Handles user registration and login using JSON Web Tokens (JWT).
* **RESTful API:** Provides a complete API for file and folder management.
* **File Operations:** Supports uploading, downloading, deleting, and moving files.
* **Folder Management:** Allows for creating, renaming, and deleting folders.
* **Secure File Sharing:** Share files or folders with other users with specific restrictions (e.g., read-only, time-limited access).
* **Object Storage Integration:** Connects directly with [MinIO](https://min.io/) (or any S3-compatible service) for efficient and scalable file storage.

---

## 🛠️ Tech Stack

* **Framework:** [Spring Boot](https://spring.io/projects/spring-boot) (Java)
* **Security:** [Spring Security](https://spring.io/projects/spring-security) & [JWT](https://jwt.io/)
* **Storage:** [MinIO Client SDK](https://min.io/docs/minio/linux/developers/java.html)
* **Database:** PostgreSQL
* **Build Tool:** [Maven](https://maven.apache.org/)

---

## 🏁 Getting Started

To get the backend server up and running on your local machine, follow these steps.

### Prerequisites

You will need the following tools installed on your system:
* [Java JDK](https://www.oracle.com/java/technologies/downloads/) (17 or later)
* [Maven](https://maven.apache.org/download.cgi)
* [MinIO Server](https://min.io/docs/minio/linux/operations/installation.html)
* (Your chosen SQL database, if not using an in-memory one like H2)

### Installation & Configuration

1.  **Clone the repository:**
    ```sh
    git clone [https://github.com/Gautam-aman/GDrive.git](https://github.com/Gautam-aman/GDrive.git)
    cd GDrive
    ```

2.  **Start your MinIO Server:**
    * Follow the MinIO documentation to start a local server.
    * Create a new bucket (e.g., `gdrive-storage`) for the application to use.

3.  **Configure the Application:**
    * Open the `src/main/resources/application.properties` file.
    * Update the `minio.*` properties with your MinIO server URL, port, access key, and secret key.
    * Configure your `spring.datasource.*` properties to connect to your database.
    * Set your JWT secret key (`jwt.secret.key`) and expiration time.

    **Example `application.properties` snippet:**
    ```properties
    # --- MinIO Configuration ---
    minio.url=[http://127.0.0.1:9000](http://127.0.0.1:9000)
    minio.access.key=YOUR_MINIO_ACCESS_KEY
    minio.secret.key=YOUR_MINIO_SECRET_KEY
    minio.bucket.name=gdrive-storage
    
    # --- Database Configuration (Example for PostgreSQL) ---
    spring.datasource.url=jdbc:postgresql://localhost:5432/gdrive_db
    spring.datasource.username=your_db_user
    spring.datasource.password=your_db_password
    spring.jpa.hibernate.ddl-auto=update
    
    # --- JWT Configuration ---
    jwt.secret.key=YourVeryStrongAndSecretKeyThatIsAtLeast256BitsLong
    jwt.expiration.ms=86400000
    ```

4.  **Run the application:**
    ```sh
    ./mvnw spring-boot:run
    ```
    The server will start, typically on `http://localhost:8080`.

---

## 📖 API Endpoints

(Recommended) You can document your main API endpoints here.

* `POST /api/auth/register` - Register a new user.
* `POST /api/auth/login` - Authenticate a user and receive a JWT.
* `POST /api/files/upload` - Upload a file.
* `GET /api/files/download/{fileId}` - Download a file.
* `DELETE /api/files/{fileId}` - Delete a file.
* `POST /api/folders` - Create a new folder.
* `POST /api/share/file/{fileId}` - Share a file with another user (e.g., send user ID and permissions in the request body).
* `GET /api/share/shared-with-me` - Get all files and folders shared with the current user.

*(This section should be expanded to match your `*Controller.java` files)*

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

---

## 📄 License

This project does not yet have a license. 

---

## 👤 Contact

Gautam-aman - [https://github.com/Gautam-aman](https://github.com/Gautam-aman)
