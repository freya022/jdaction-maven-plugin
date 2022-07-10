import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Test {
	public static void main(String[] args) throws Exception {
		JDA jda = JDABuilder.createLight("")
				.build()
				.awaitReady();

		jda.retrieveUserById(0);
	}

	public void testTypes() {
		Guild g = null;

		g.retrieveAuditLogs();

		g.updateCommands();

		g.modifyRolePositions();
	}

	public class Inner {
		public void test() throws Exception {
			JDA jda = JDABuilder.createLight("")
					.build()
					.awaitReady();

			jda.retrieveUserById(0);
		}
	}

	public record InnerRecord() {
		public void test() throws Exception {
			JDA jda = JDABuilder.createLight("")
					.build()
					.awaitReady();

			jda.retrieveUserById(0);
		}
	}
}
